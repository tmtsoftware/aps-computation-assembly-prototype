package aps.computationprototypeassembly.commands;

import csw.params.commands.Result;
import csw.params.core.models.Id;
import csw.params.core.models.ArrayData;
import csw.params.core.generics.Parameter;
import csw.params.javadsl.JKeyType;
import aps.computationprototypeassembly.AlgorithmLibrary;
import aps.computationprototypeassembly.Configuration;
import aps.computationprototypeassembly.Results;
import aps.computationprototypeassembly.metadata.ComputationParameter;
import aps.computationprototypeassembly.metadata.ComputationUtils;

import java.util.List;

public class ExecuteColorStep implements WorkerCommand {

    private final Id runId;
    private final int stepCount;
    private final float stepSizeNm;

    private static final List<ComputationParameter> metadata = List.of(
        new ComputationParameter("stepCount", int.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("stepSizeNm", float.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("colorSteps", float.class, new int[]{0,0}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.OUTPUT)
    );

    public ExecuteColorStep(Id runId, int stepCount, float stepSizeNm) {
        this.runId = runId;
        this.stepCount = stepCount;
        this.stepSizeNm = stepSizeNm;
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

        library.colorStep((int) argsForFortran[0],
            (float) argsForFortran[1],
            (float[][]) argsForFortran[2]);

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

        // testing/debugging
        results.printAll();

        return new Result();
    }
}
