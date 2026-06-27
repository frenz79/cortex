package com.cortex.base.soa.logic;

public interface IPlasticityLogic {

	 boolean onPreSpike ( long now, int synId );
	 boolean onPostSpike( long now, int synId, long postTime, float postRate);
	 void applyReward(long now, int synId, float reward, float neuromodulator);
	 float update(long now, int synId, float postRate);
	 boolean hadSignificantPairing(int synId);
	 float getWeight(int synId);
	 boolean isEligible(long now, int synId, long window);
	 
}
