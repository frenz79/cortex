package com.cortex.base;

import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;

public class InhibitorySynapticPlasticity implements IPlasticityRule {

	public static record InhibitorySynapticPlasticityConfig( 
		float LEARNING_RATE,
		float TARGET_FIRING_RATE,
		float W_MIN,
		float W_MAX,
		long RATE_WINDOW){
	}

	private final InhibitorySynapticPlasticityConfig config;
    private float weight;
    
    public InhibitorySynapticPlasticity(float initialWeight, InhibitorySynapticPlasticityConfig config) {
        this.weight = initialWeight;
        this.config = config;
    }
    
	@Override
	public void onPostSpike(Synapse s, long dt, long now) {
		float postRate = s.getTarget().getRecentFiringRate(now);
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        this.weight = Maths.clamp(this.weight + dw, config.W_MIN, config.W_MAX);
	}

	@Override
	public float getWeight() {
		return weight;
	}

	@Override
	public void onPreSpike(long dt) {
		// inibitori NON rinforzano su pre
	}

	@Override
	public void update(long now) {
		// TODO Auto-generated method stub
		
	}

}
