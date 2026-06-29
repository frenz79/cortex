package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public final class DendriticCompetitionParamsSoA {

    // WinnerTakeMost
	@SerializableAttribute
    public final float[] wtm_inhibitionLevel;
	@SerializableAttribute
    public final long[]  wtm_tauNanos;

    // Continuous
	@SerializableAttribute
    public final long[]  cont_tauNanos;
	@SerializableAttribute
    public final float[] cont_inhibitionMax;

    // Normalized
	@SerializableAttribute
    public final float[] norm_strength;
	@SerializableAttribute
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
