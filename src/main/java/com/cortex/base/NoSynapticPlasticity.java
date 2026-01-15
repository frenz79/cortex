package com.cortex.base;

import com.cortex.commons.IPlasticityRule;

public class NoSynapticPlasticity implements IPlasticityRule {

	public static IPlasticityRule SINGLETON_INSTANCE = new NoSynapticPlasticity();
		
	@Override
	public void onPreSpike(long dt) {
		
	}

	@Override
	public void onPostSpike(Synapse s, long dt, long now) {
		
	}

	@Override
	public float getWeight() {
		return 0.2f;
	}

}
