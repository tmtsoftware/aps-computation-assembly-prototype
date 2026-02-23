package aps.computationprototypeassembly;

public class Results {

    float[] centroidOffsetsX = new float[36];
    float[] centroidOffsetsY = new float[36];
    float[] xOffsetsOut;
    float[] yOffsetsOut;
    float[][] tipTiltActs;
    float[][] pistonActs;
    float[][] desiredActDeltas;


    public Results() {
        // in out test, we have these pre-initialized from a 'previous step'
        for (int i=0; i<36; i++) {
            centroidOffsetsX[i] = 0.0f;
            centroidOffsetsY[i] = 0.0f;
        }
    }
}
