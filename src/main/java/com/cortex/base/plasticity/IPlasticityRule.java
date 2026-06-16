package com.cortex.base.plasticity;

import com.cortex.base.Synapse;

public interface IPlasticityRule {
	 float update(long now, Synapse s);
	 boolean onPreSpike( long dt );
	 boolean onPostSpike( Synapse s, long dt, long now );
	 float getWeight();
	 void applyReward(float r, long t, float neuromodulator);
	 boolean isEligible(long now, long window);
	 boolean hadSignificantPairing();
}
