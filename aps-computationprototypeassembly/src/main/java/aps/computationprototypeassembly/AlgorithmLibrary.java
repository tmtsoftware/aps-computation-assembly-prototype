package aps.computationprototypeassembly;

import csw.params.core.models.Semester;
import csw.params.core.models.Semester;
import org.tmt.aps.peas.lang.interop.JcolorStep;
import org.tmt.aps.peas.lang.interop.RetVal;
import org.tmt.aps.peas.lang.interop.JcalculateM2M1RayTrace;
import org.tmt.aps.peas.lang.interop.JttOffsetsToActs;
import org.tmt.aps.peas.lang.interop.JdecomposeActs;
import csw.framework.models.JCswContext;
import csw.logging.api.javadsl.ILogger;

public class AlgorithmLibrary {

    private final ILogger log;

    public AlgorithmLibrary(JCswContext cswCtx) {
        this.log = cswCtx.loggerFactory().getLogger(getClass());
    }
    public float[][] colorStep(int stepCount, float stepSizeNm) throws Exception {

        JcolorStep jcolorStep = new JcolorStep();
        RetVal retVal = new RetVal();

        float[][] colorSteps = new float[stepCount + 1][3];

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
        float[] y_offsets = results.centroidOffsetsY;
        ;

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

        results.xOffsetsOut = x_offsets_out;
        results.yOffsetsOut = y_offsets_out;

        // store fi_param values
        float[][] desiredActDeltas = new float[configuration.actuatorPositionsX.length/3][3];
        for (int i = 0; i<configuration.actuatorPositionsX.length/3; i++) {
            desiredActDeltas[i][0] = desired_act_deltas[i*3 + 0];
            desiredActDeltas[i][1] = desired_act_deltas[i*3 + 1];
            desiredActDeltas[i][2] = desired_act_deltas[i*3 + 2];
        }

        results.desiredActDeltas = desiredActDeltas;

    }

    public void decomposeActs(Configuration configuration, Results results) throws Exception {

        log.info("in decomposeActs");
        JdecomposeActs jdecomposeActs = new JdecomposeActs();
        RetVal retVal = new RetVal();
        log.info("decomposeActs::flatten2dArray");
        float[] actPos = flatten2dArray(results.desiredActDeltas, 1);

        // output arrays
        log.info("decomposeActs::act_tt allocation");
        float[] act_tt = new float[actPos.length];
        log.info("decomposeActs::act_tt allocation");
        float[] act_p = new float[actPos.length];


        log.info("decomposeActs::calling Fortran");
        Object output[] = jdecomposeActs.jdecomposeActs(retVal, actPos, act_tt, act_p);

        if (retVal.getCode() > 0) {
            throw new Exception("Decompose Actuators Calculation Error:  " + retVal);
        }

        // store _param values


        results.tipTiltActs = expandTo2dArray(act_tt, 3);
        results.pistonActs = expandTo2dArray(act_p, 3);
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