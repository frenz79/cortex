package com.cortex.base.soa;

public final class DendriticCompetitionParamsSoA {

    // WinnerTakeMost
    public final float[] wtm_inhibitionLevel;
    public final long[]  wtm_tauNanos;

    // Continuous
    public final long[]  cont_tauNanos;
    public final float[] cont_inhibitionMax;

    // Normalized
    public final float[] norm_strength;
    public final float[] norm_stability;

    public DendriticCompetitionParamsSoA(int totalBranchTypes) {
        wtm_inhibitionLevel = new float[totalBranchTypes];
        wtm_tauNanos        = new long[totalBranchTypes];

        cont_tauNanos       = new long[totalBranchTypes];
        cont_inhibitionMax  = new float[totalBranchTypes];

        norm_strength       = new float[totalBranchTypes];
        norm_stability      = new float[totalBranchTypes];
    }
}
