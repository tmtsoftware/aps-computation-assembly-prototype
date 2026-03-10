package aps.computationprototypeassembly.commands;

import aps.computationprototypeassembly.Constants;
import aps.computationprototypeassembly.metadata.ComputationParameter;
import csw.params.commands.Result;
import csw.params.core.models.Id;
import csw.params.commands.ControlCommand;
import csw.params.commands.Setup;
import csw.params.core.generics.Key;
import aps.computationprototypeassembly.AlgorithmLibrary;
import aps.computationprototypeassembly.Configuration;
import aps.computationprototypeassembly.Results;
import aps.computationprototypeassembly.metadata.ComputationUtils;
import csw.params.javadsl.JKeyType;

import java.util.List;
import java.util.Optional;


public class ExecuteDecomposeActs implements WorkerCommand {

    private final Id runId;
    private Setup setup;

    private static final List<ComputationParameter> metadata = List.of(
            new ComputationParameter("desiredActDeltas", float.class, new int[]{36,3}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
            new ComputationParameter("tipTiltActs", float.class, new int[]{108}, ComputationParameter.Destination.RESULTS, ComputationParameter.Direction.OUTPUT),
            new ComputationParameter("pistonActs", float.class, new int[]{108}, ComputationParameter.Destination.RESULTS, ComputationParameter.Direction.OUTPUT)
    );
    public ExecuteDecomposeActs(Id runId, ControlCommand controlCommand) {
        this.runId = runId;
        setup = (Setup) controlCommand;
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
        library.decomposeActs((float[][])argsForFortran[0], (float[])argsForFortran[1], (float[])argsForFortran[2]);

       // populate results data cache
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
        Thread.sleep(1000);
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

