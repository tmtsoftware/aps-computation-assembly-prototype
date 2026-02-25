package aps.computationprototypeassembly.commands;

import aps.computationprototypeassembly.metadata.ComputationParameter;
import csw.params.commands.Result;
import csw.params.core.models.Id;
import aps.computationprototypeassembly.AlgorithmLibrary;
import aps.computationprototypeassembly.Configuration;
import aps.computationprototypeassembly.Results;
import aps.computationprototypeassembly.metadata.ComputationParameter;
import aps.computationprototypeassembly.metadata.ComputationUtils;
import java.util.List;


public class ExecuteTtOffsetsToActs implements WorkerCommand {

    private final Id runId;

    private static final List<ComputationParameter> metadata = List.of(
            new ComputationParameter("actuatorPositionsX", float.class, new int[]{108}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("actuatorPositionsY", float.class, new int[]{108}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("secPerPix", float.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("centroidOffsetsX", float.class, new int[]{36}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
            new ComputationParameter("centroidOffsetsY", float.class, new int[]{36}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
            new ComputationParameter("mirrorConfig", int.class, new int[]{36}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
            new ComputationParameter("xOffsetsOut", float.class, new int[]{36}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.OUTPUT),
            new ComputationParameter("yOffsetsOut", float.class, new int[]{36}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.OUTPUT),
            new ComputationParameter("desiredActDeltas", float.class, new int[]{36,3}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.OUTPUT)
    );

    public ExecuteTtOffsetsToActs(Id runId) {
        this.runId = runId;
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
                argsForFortran[i] = p.source == ComputationParameter.Source.CONFIGURATION
                        ? config.get(p.name)
                        : results.get(p.name);
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
                if (p.source == ComputationParameter.Source.RESULTS) {
                    results.set(p.name, argsForFortran[i]);
                } else {
                    throw new Exception("output should be in RESULTS metadata");
                }
            }
        }

        return new Result();

    }
}
