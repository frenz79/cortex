package com.cortex.commons;

import com.cortex.base.Synapse;

public interface IPlasticityRule {
	 void update(long now, Synapse s);
	 boolean onPreSpike( long dt );
	 boolean onPostSpike( Synapse s, long dt, long now );
	 float getWeight();
	 void applyReward(float r, long t, float neuromodulator);
	 void updateDelay(float r);
	 boolean isEligible(long now, long window);
}
