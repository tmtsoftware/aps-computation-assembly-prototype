package aps.computationprototypeassembly;

import org.tmt.aps.peas.lang.interop.JcolorStep;
import org.tmt.aps.peas.lang.interop.RetVal;
import org.tmt.aps.peas.lang.interop.JttOffsetsToActs;
import org.tmt.aps.peas.lang.interop.JdecomposeActs;
import csw.framework.models.JCswContext;
import csw.logging.api.javadsl.ILogger;

public class AlgorithmLibrary {

    private final ILogger log;

    public AlgorithmLibrary(JCswContext cswCtx) {
        this.log = cswCtx.loggerFactory().getLogger(getClass());
    }
    public void colorStep(int stepCount, float stepSizeNm, float[][] colorsteps) throws Exception {

        JcolorStep jcolorStep = new JcolorStep();
        RetVal retVal = new RetVal();

        float[][] colorSteps = new float[stepCount + 1][3];

        double t1 = System.nanoTime();

        Object[] result = jcolorStep.jcolorStep(retVal, stepCount, stepSizeNm, colorSteps);

        if (retVal.getCode() > 0) {
            throw new Exception("colorStep calcuation error. " + retVal + ".  ");
        }
    }

    // method call with formal parameter names
    public void ttOffsetsToActs(float[] xActPos, float[] yActPos, float secPerPix,
       float[] centroidOffsetsX, float[] centroidOffsetsY, int[] mirrorConfig, float[] xOffsetsOut, float[] yOffsetsOut,
       float[][] desiredActDeltas) throws Exception {

        JttOffsetsToActs jttOffsetsToActs = new JttOffsetsToActs();
        RetVal retVal = new RetVal();

        double t1 = System.nanoTime();

        // input vars (mostly just shifting from Algorithm Library Formal param names to Fortran names
        float[] x_act_pos = xActPos;
        float[] y_act_pos = yActPos;
        float secperpix = secPerPix;
        int[] mirror_config = mirrorConfig;
        float[] x_offsets = centroidOffsetsX;
        float[] y_offsets = centroidOffsetsY;

        // output arrays
        float[] desired_act_deltas = new float[xActPos.length];  // not final output var, need to pre-allocate
        float[] x_offsets_out = xOffsetsOut;  // pre-allocated
        float[] y_offsets_out = yOffsetsOut;  // pre-allocated

        // fortran call with slightly different parameters
        Object output[] = jttOffsetsToActs.jttOffsetsToActs(retVal, x_act_pos, y_act_pos, secperpix, x_offsets, y_offsets,
                mirror_config, x_offsets_out, y_offsets_out, desired_act_deltas);

        if (retVal.getCode() > 0) {
            throw new Exception("\"Tip/Tilt Offsets to Actuator Calculation Error. " + retVal + ".  ");
        }

        // move fortran output params to method formal params
        xOffsetsOut = x_offsets_out;
        yOffsetsOut = y_offsets_out;

        // store fi_param values into preallocated desiredActDeltas
        for (int i = 0; i<xActPos.length/3; i++) {
            desiredActDeltas[i][0] = desired_act_deltas[i*3 + 0];
            desiredActDeltas[i][1] = desired_act_deltas[i*3 + 1];
            desiredActDeltas[i][2] = desired_act_deltas[i*3 + 2];
        }

    }

    public void decomposeActs(float[][] desiredActDeltas, float[] tipTiltActs, float[] pistonActs) throws Exception {

        log.info("in decomposeActs");
        JdecomposeActs jdecomposeActs = new JdecomposeActs();
        RetVal retVal = new RetVal();

        float[] actPos = flatten2dArray(desiredActDeltas, 1);

        // output arrays

        float[] act_tt = tipTiltActs;

        float[] act_p = pistonActs;



        Object output[] = jdecomposeActs.jdecomposeActs(retVal, actPos, act_tt, act_p);

        if (retVal.getCode() > 0) {
            throw new Exception("Decompose Actuators Calculation Error:  " + retVal);
        }

    }


    public static float[] flatten2dArray(float[][] input, int fastIndex) {
        float[] result = new float[input.length * input[0].length];
        int k = 0;
        if (fastIndex == 0) {
            for (int i = 0; i < input[0].length; i++) {
                for (int j = 0; j < input.length; j++) {
                    result[k++] = input[j][i];
                }
            }
        } else {
            for (int i = 0; i < input.length; i++) {
                for (int j = 0; j < input[0].length; j++) {
                    result[k++] = input[i][j];
                }
            }
        }
        return result;
    }

    public static float[][] expandTo2dArray(float[] input, int minorIndexSize) {
        float[][] result = new float[input.length / minorIndexSize][minorIndexSize];
        for (int i = 0; i < input.length / minorIndexSize; i++) {
            for (int j = 0; j < minorIndexSize; j++) {
                result[i][j] = input[i * minorIndexSize + j];
            }
        }
        return result;
    }

}