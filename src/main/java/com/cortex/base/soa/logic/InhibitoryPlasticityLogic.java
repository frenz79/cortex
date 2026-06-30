package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.utils.Maths;

public final class InhibitoryPlasticityLogic implements IPlasticityLogic {

	private final PlasticitySoA plasticitySoA;

	public InhibitoryPlasticityLogic( PlasticitySoA plasticitySoA ) {
		Objects.nonNull(plasticitySoA);

		this.plasticitySoA = plasticitySoA;
	}

	@Override
	public float getWeight(int synId) {
		return plasticitySoA.weight[synId];
	}
	
	 /**
     * Homeostasis pura:
     * w += homeostaticRate * (postRate - wBaseline)
     */
    @Override
    public float update(long now, 
    					int synId,
                        float postRate,
                        float homeostaticRateOrLearningRate,
                        float wBaselineOrTargetFiringRate,
                        float wMin,
                        float wMax,
                        long eligibilityDecayNanos) {
		float error = postRate - wBaselineOrTargetFiringRate;
		float dw = homeostaticRateOrLearningRate * error;
		float w = plasticitySoA.weight[synId] + dw;
		plasticitySoA.weight[synId] = Maths.clamp(w, wMin, wMax);
		return w;
    }
    
	@Override
	public boolean onPostSpike(long now, int synId, long postTime, float postRate, float tauPlusOrLearningRate, float tauMinsOrTargetFiringRate, float aPlusOrWMin, float aMinusOrWMax) {
		update(
			now, synId, postRate, tauPlusOrLearningRate, tauMinsOrTargetFiringRate, aPlusOrWMin, aMinusOrWMax, 0l
		);
		return false;
	}

	@Override
	public void applyReward(long now, int synId, float reward, float neuromodulator, float wMin, float wMax) {
		// no-op
	}

	@Override
	public boolean isEligible(long now, int synId, long window) {
		return false;
	}

	@Override
	public boolean hadSignificantPairing(int synId, float tauPlus, float tauMins, float aPlus, float aMinus) {
		return false;
	}

	@Override
	public boolean onPreSpike(long now, int synId, float tauPlus, float tauMins, float aPlus, float aMinus) {
		return false;
	}
}
