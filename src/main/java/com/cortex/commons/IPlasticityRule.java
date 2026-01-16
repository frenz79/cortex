package com.cortex.commons;

import com.cortex.base.Synapse;

public interface IPlasticityRule {
	 void update(long now);
	 void onPreSpike( long dt );
	 void onPostSpike( Synapse s, long dt, long now );
	 float getWeight();
	 void applyReward(float r, long t);
	 void updateDelay(float r);
}
