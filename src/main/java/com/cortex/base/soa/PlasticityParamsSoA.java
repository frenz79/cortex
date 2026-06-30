package com.cortex.base.soa;

public class PlasticityParamsSoA {
	public final float[] tauPlusOrLearningRate;
    public final float[] tauMinusOrTargetFiringRate;
    public final float[] aPlusOrWMin;
    public final float[] aMinusOrWMax;

    public final float[] homeostaticRateOrLearningRate;
    public final float[] wBaselineOrTargetRate;
    public final float[] wMin;
    public final float[] wMax;

    public final long[] eligibilityDecayNanos;

    public PlasticityParamsSoA(int totalSynapses) {
        tauPlusOrLearningRate = new float[totalSynapses];
        tauMinusOrTargetFiringRate = new float[totalSynapses];
        aPlusOrWMin = new float[totalSynapses];
        aMinusOrWMax = new float[totalSynapses];

        homeostaticRateOrLearningRate = new float[totalSynapses];
        wBaselineOrTargetRate = new float[totalSynapses];
        wMin = new float[totalSynapses];
        wMax = new float[totalSynapses];

        eligibilityDecayNanos = new long[totalSynapses];
    }
}
