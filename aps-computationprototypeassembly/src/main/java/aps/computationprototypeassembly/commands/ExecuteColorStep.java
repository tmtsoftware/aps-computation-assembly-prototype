package aps.computationprototypeassembly.commands;

import aps.computationprototypeassembly.Constants;
import csw.params.commands.Result;
import csw.params.core.models.Id;
import csw.params.core.models.ArrayData;
import csw.params.core.generics.Parameter;
import csw.params.commands.ControlCommand;
import csw.params.commands.Setup;
import csw.params.javadsl.JKeyType;
import csw.params.core.generics.Key;
import aps.computationprototypeassembly.AlgorithmLibrary;
import aps.computationprototypeassembly.Configuration;
import aps.computationprototypeassembly.Results;
import aps.computationprototypeassembly.metadata.ComputationParameter;
import aps.computationprototypeassembly.metadata.ComputationUtils;

import java.util.List;
import java.util.Optional;

public class ExecuteColorStep implements WorkerCommand {

    private final Id runId;
    private Setup setup;

    private static List<ComputationParameter> metadata = List.of(
        new ComputationParameter("stepCount", int.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("stepSizeNm", float.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("colorSteps", float.class, new int[]{12,3}, ComputationParameter.Destination.RESULTS, ComputationParameter.Direction.OUTPUT)
    );

    public ExecuteColorStep(Id runId, ControlCommand controlCommand) {
        this.runId = runId;
        setup = (Setup)controlCommand;
        Optional<?> setupValue = extractFromSetup(metadata.get(0));
        int stepCount = (int)setupValue.get();
        // set the size of the output array which is based on inputs
        metadata.get(2).dimensions = new int[]{stepCount+1, 3};
    }

    @Override
    public Id runId() {
        return runId;
    }

    @Override
    public Result execute(AlgorithmLibrary library) throws Exception {

        Results results = Results.getInstance();
        Configuration config = Configuration.getInstance();
        Constants constants = Constants.getInstance();

        // Prepare argument array in exact metadata order
        Object[] argsForFortran = new Object[metadata.size()];

        for (int i = 0; i < metadata.size(); i++) {
            ComputationParameter p = metadata.get(i);
            if (p.direction == ComputationParameter.Direction.INPUT) {
                // Read input from the appropriate source
                // Add logic to see if argument name is present in Setup command, and if so, override argsForFortran value with the one from the command
                Object value = null;

                // Try to override with Setup value if present
                Optional<?> setupValue = extractFromSetup(p);

                if (setupValue.isPresent()) {
                    // we have a setup override, either use results name or actual value
                    Object overrideValue = setupValue.get();

                    if (overrideValue instanceof String) {
                        // use reference value from cmd
                        String referenceKey = (String) overrideValue;
                        value = results.get(referenceKey);
                    } else {
                        // use value directly from command
                        value = setupValue.get();
                    }

                } else if (p.source == ComputationParameter.Source.CONFIGURATION) {
                    value = config.get(p.name);
                } else if (p.source == ComputationParameter.Source.CONSTANT){
                    value = constants.get(p.name);
                }
                argsForFortran[i] = value;

            } else {
                // Allocate output container of the correct type and shape
                argsForFortran[i] = ComputationUtils.allocateArray(p.type, p.dimensions);
            }
        }

        System.out.println("argsForFortran[0] = " + argsForFortran[0]);
        System.out.println("argsForFortran[1] = " + argsForFortran[1]);
        System.out.println("argsForFortran[2] = " + ((float[][]) argsForFortran[2]));

        library.colorStep((int) argsForFortran[0],
            (float) argsForFortran[1],
            (float[][]) argsForFortran[2]);

        System.out.println("argsForFortran[2] = " + ((float[][]) argsForFortran[2])[4][1]);

        // Populate outputs into Results if needed
        for (int i = 0; i < metadata.size(); i++) {
            ComputationParameter p = metadata.get(i);
            if (p.direction == ComputationParameter.Direction.OUTPUT) {
                if (p.destination == ComputationParameter.Destination.RESULTS) {
                    String key = setup.commandName().name() + "." + p.name;
                    results.set(key, argsForFortran[i]);
                } else {
                    throw new Exception("output should be in RESULTS metadata");
                }
            }
        }

        // testing/debugging
        results.printAll();

        return new Result();
    }


    // -----------------------------------------
    // Helper: Extract parameter from Setup
    // -----------------------------------------
    private Optional<?> extractFromSetup(ComputationParameter p) {

        try {
            // Metadata-driven types
            if (p.type == int.class) {
                Key<Integer> key = JKeyType.IntKey().make(p.name);
                Optional<?> result = setup.jGet(key).map(param -> param.head());
                if (result.isPresent()) return result;
            }

            if (p.type == float.class) {
                Key<Float> key = JKeyType.FloatKey().make(p.name);
                Optional<?> result = setup.jGet(key).map(param -> param.head());
                if (result.isPresent()) return result;
            }

            // 🔹 NEW: fallback to String if present in command
            Key<String> stringKey = JKeyType.StringKey().make(p.name);
            Optional<?> stringResult = setup.jGet(stringKey).map(param -> param.head());
            if (stringResult.isPresent()) return stringResult;

        } catch (Exception ignored) {
        }

        return Optional.empty();
    }

}
