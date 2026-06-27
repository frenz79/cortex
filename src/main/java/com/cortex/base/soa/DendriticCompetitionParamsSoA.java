package com.cortex.base.soa;

public final class DendriticCompetitionParamsSoA {

    // WinnerTakeMost
    public float[] wtm_inhibitionLevel;
    public long[]  wtm_tauNanos;

    // Continuous
    public long[]  cont_tauNanos;
    public float[] cont_inhibitionMax;

    // Normalized
    public float[] norm_strength;
    public float[] norm_stability;

    public DendriticCompetitionParamsSoA(int totalBranchTypes) {
        wtm_inhibitionLevel = new float[totalBranchTypes];
        wtm_tauNanos        = new long[totalBranchTypes];

        cont_tauNanos       = new long[totalBranchTypes];
        cont_inhibitionMax  = new float[totalBranchTypes];

        norm_strength       = new float[totalBranchTypes];
        norm_stability      = new float[totalBranchTypes];
    }
}
