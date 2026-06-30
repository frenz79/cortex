package com.cortex.base.soa.logic;

public interface IPlasticityLogic {

	 float getWeight( int synId );
	 
	 boolean isEligible(
		long now, 
		int synId, 
		long window);
	 
	 void applyReward(
		long now, 
		int synId, 
		float reward, 
		float neuromodulator, 
		float wMin, 
		float wMax);

	 boolean hadSignificantPairing(
		int synId, 
		float tauPlus, 
		float tauMins, 
		float aPlus, 
		float aMinus);
	 
	 boolean onPreSpike(
		long now, 
		int synId, 
		float tauPlus, 
		float tauMins, 
		float aPlus, 
		float aMinus);

	 boolean onPostSpike(
		long now, 
		int synId, 
		long postTime, 
		float postRate, 
		float tauPlusOrLearningRate, 
		float tauMinusOrTargetFiringRate, 
		float aPlusOrWMin, 
		float aMinusOrWMax);
	 
	 float update(
		long now, 
		int synId,
        float postRate,
        float homeostaticRateOrLearningRate,
        float wBaselineOrTargetFiringRate,
        float wMin,
        float wMax,
        long eligibilityDecayNanos);
}
