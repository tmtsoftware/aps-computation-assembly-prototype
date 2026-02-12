package aps.computationprototypeassembly;

import org.tmt.aps.peas.lang.interop.JcolorStep;
import org.tmt.aps.peas.lang.interop.RetVal;


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



}
