package aps.computationprototypeassembly.commands;

import aps.computationprototypeassembly.metadata.ComputationParameter;
import csw.params.commands.Result;
import csw.params.core.models.Id;
import aps.computationprototypeassembly.AlgorithmLibrary;
import aps.computationprototypeassembly.Configuration;
import aps.computationprototypeassembly.Results;
import aps.computationprototypeassembly.metadata.ComputationUtils;

import java.util.List;


public class ExecuteDecomposeActs implements WorkerCommand {

    private final Id runId;

    private static final List<ComputationParameter> metadata = List.of(
            new ComputationParameter("desiredActDeltas", float.class, new int[]{36,3}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
            new ComputationParameter("tipTiltActs", float.class, new int[]{108}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.OUTPUT),
            new ComputationParameter("pistonActs", float.class, new int[]{108}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.OUTPUT)
    );
    public ExecuteDecomposeActs(Id runId) {
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
        library.decomposeActs((float[][])argsForFortran[0], (float[])argsForFortran[1], (float[])argsForFortran[2]);

       // populate results data cache
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

        // testing/debugging
        Thread.sleep(1000);
        results.printAll();

        return new Result();
    }
}

