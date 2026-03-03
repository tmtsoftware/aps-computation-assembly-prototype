
package aps.computationprototypeassembly.commands;

import aps.computationprototypeassembly.metadata.ComputationParameter;
import csw.params.commands.*;
import csw.params.core.models.*;
import aps.computationprototypeassembly.AlgorithmLibrary;
import aps.computationprototypeassembly.Configuration;
import aps.computationprototypeassembly.Results;
import aps.computationprototypeassembly.metadata.ComputationUtils;

import java.util.List;


public class ExecuteCalculatePupilRegError implements WorkerCommand {

    private final Id runId;

    int numSpots = 508; // will be gereralized for differnet cmd calls

    private final List<ComputationParameter> metadata = List.of(
        new ComputationParameter("fractionalIntensityCalcMethod", float.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("intensities", float.class, new int[]{numSpots}, ComputationParameter.Source.RESULTS, ComputationParameter.Direction.INPUT),
        new ComputationParameter("numSpots", int.class, new int[]{}, ComputationParameter.Source.COMMAND, ComputationParameter.Direction.INPUT),
        new ComputationParameter("peripheralSpotPerp", float.class, new int[]{36}, ComputationParameter.Source.CONSTANT, ComputationParameter.Direction.INPUT),
        new ComputationParameter("peripheralSpotParallel", float.class, new int[]{36}, ComputationParameter.Source.CONSTANT, ComputationParameter.Direction.INPUT),
        new ComputationParameter("peripheralSpotTheta", float.class, new int[]{36}, ComputationParameter.Source.CONSTANT, ComputationParameter.Direction.INPUT),
        new ComputationParameter("aHex", float.class, new int[]{}, ComputationParameter.Source.CONSTANT, ComputationParameter.Direction.INPUT),
        new ComputationParameter("spotDiameter", float.class, new int[]{}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("nspotTypes", int.class, new int[]{numSpots}, ComputationParameter.Source.CONSTANT, ComputationParameter.Direction.INPUT),
        new ComputationParameter("missingSpotFlags", int.class, new int[]{numSpots}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("findCentStatusList", int.class, new int[]{numSpots}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("pupilRegistrationOffsetX", float.class, new int[]{numSpots}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),
        new ComputationParameter("pupilRegistrationOffsetY", float.class, new int[]{numSpots}, ComputationParameter.Source.CONFIGURATION, ComputationParameter.Direction.INPUT),

        new ComputationParameter("regErrorX", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT),
        new ComputationParameter("regErrorY", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT),
        new ComputationParameter("regErrorPhi", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT),
        new ComputationParameter("regErrorApproxX", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT),
        new ComputationParameter("regErrorApproxY", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT),
        new ComputationParameter("regErrorApproxPhi", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT),
        new ComputationParameter("regScaleError", float.class, new int[]{}, ComputationParameter.Destination.RESPONSE, ComputationParameter.Direction.OUTPUT)
    );

    public ExecuteCalculatePupilRegError(Id runId) {
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
                if (p.destination == ComputationParameter.Destination.RESULTS) {
                    results.set(p.name, argsForFortran[i]);
                } else if (p.destination == ComputationParameter.Destination.RESPONSE) {
                    // TODO: return as RESPONSE variables

                } else {
                    throw new Exception("output destination not RESULTS or RESPONSE");
                }
            }
        }

        // testing/debugging
        Thread.sleep(1000);
        results.printAll();

        return new Result();
    }
}









