package com.cortex.commons;

import com.cortex.base.Synapse;

public interface IPlasticityRule {
	
	 void onPreSpike( long dt );
	 void onPostSpike( Synapse s, long dt, long now );
	 float getWeight();
}
