package aps.computationprototypeassembly;

import org.tmt.aps.peas.lang.interop.JcolorStep;
import org.tmt.aps.peas.lang.interop.RetVal;
import org.tmt.aps.peas.lang.interop.JcalculateM2M1RayTrace;
import org.tmt.aps.peas.lang.interop.JttOffsetsToActs;
import org.tmt.aps.peas.lang.interop.JdecomposeActs;

public class AlgorithmLibrary {

    public float[][] colorStep(int stepCount, float stepSizeNm) throws Exception {

        JcolorStep jcolorStep = new JcolorStep();
        RetVal retVal = new RetVal();

        float[][] colorSteps = new float[stepCount+1][3];

        double t1 = System.nanoTime();

        Object[] result = jcolorStep.jcolorStep(retVal, stepCount, stepSizeNm, colorSteps);

        if (retVal.getCode() > 0) {
            throw new Exception("colorStep calcuation error. " + retVal + ".  ");
        }

        return colorSteps;
    }

    public void ttOffsetsToActs(Configuration configuration, Results results) throws Exception {

        JttOffsetsToActs jttOffsetsToActs = new JttOffsetsToActs();
        RetVal retVal = new RetVal();

        double t1 = System.nanoTime();

        float secperpix = configuration.secPerPix;
        float[] x_act_pos = configuration.actuatorPositionsX;
        float[] y_act_pos = configuration.actuatorPositionsY;

        float[] x_offsets = results.centroidOffsetsX;
        float[] y_offsets = results.centroidOffsetsY;;

        int[] mirror_config = configuration.mirrorConfig;

        // output arrays
        float[] desired_act_deltas = new float[configuration.actuatorPositionsX.length];
        float[] x_offsets_out = new float[results.centroidOffsetsX.length];
        float[] y_offsets_out = new float[results.centroidOffsetsY.length];

        Object output[] = jttOffsetsToActs.jttOffsetsToActs(retVal, x_act_pos, y_act_pos, secperpix, x_offsets, y_offsets,
                mirror_config, x_offsets_out, y_offsets_out, desired_act_deltas);

        if (retVal.getCode() > 0) {
            throw new Exception("\"Tip/Tilt Offsets to Actuator Calculation Error. " + retVal + ".  ");
        }

        results.desiredActDeltas = desired_act_deltas;
        results.xOffsetsOut = x_offsets_out;
        results.yOffsetsOut = y_offsets_out;
    }



}
