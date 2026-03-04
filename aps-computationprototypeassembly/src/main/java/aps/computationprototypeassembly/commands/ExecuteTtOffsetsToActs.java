package aps.computationprototypeassembly.commands;

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


public class ExecuteTtOffsetsToActs implements WorkerCommand {

    private final Id runId;
    private Setup setup;

    private static final List<ComputationParameter> metadata = List.of(
            new ComputationParameter("actuatorPositionsX", float.class, new int[]{108}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("actuatorPositionsY", float.class, new int[]{108}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("secPerPix", float.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("centroidOffsetsX", float.class, new int[]{36}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
            new ComputationParameter("centroidOffsetsY", float.class, new int[]{36}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
            new ComputationParameter("mirrorConfig", int.class, new int[]{36}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("xOffsetsOut", float.class, new int[]{36}, ComputationParameter.Destination.RESULTS, ComputationParameter.Direction.OUTPUT),
            new ComputationParameter("yOffsetsOut", float.class, new int[]{36}, ComputationParameter.Destination.RESULTS, ComputationParameter.Direction.OUTPUT),
            new ComputationParameter("desiredActDeltas", float.class, new int[]{36,3}, ComputationParameter.Destination.RESULTS, ComputationParameter.Direction.OUTPUT)
    );

    public ExecuteTtOffsetsToActs(Id runId, ControlCommand controlCommand) {
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

                } else {

                    // Read input from the appropriate source
                    value = p.source == ComputationParameter.Source.CONFIGURATION
                            ? config.get(p.name)
                            : results.get(p.name);
                }
                argsForFortran[i] = value;

            } else {
                // Allocate output container of the correct type and shape
                argsForFortran[i] = ComputationUtils.allocateArray(p.type, p.dimensions);
            }
        }

        library.ttOffsetsToActs(
                (float[])argsForFortran[0],
                (float[])argsForFortran[1],
                (float)argsForFortran[2],
                (float[])argsForFortran[3],
                (float[])argsForFortran[4],
                (int[])argsForFortran[5],
                (float[])argsForFortran[6],
                (float[])argsForFortran[7],
                (float[][])argsForFortran[8]);

        // Populate outputs into Results or Configuration if needed
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
