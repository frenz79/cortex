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
	public boolean onPostSpike(Synapse s, long dt, long now) {
		float postRate = s.getTarget().getRecentFiringRate(now);
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        this.weight = Maths.clamp(this.weight + dw, config.W_MIN, config.W_MAX);
        return false;
	}

	@Override
	public float getWeight() {
		return weight;
	}

	@Override
	public boolean onPreSpike(long dt) {
		// inibitori NON rinforzano su pre
		return false;
	}

	@Override
	public void update(long now) {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateDelay(float r) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean isEligible(long now, long window) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void applyReward(float r, long t, float neuromodulator) {
		// TODO Auto-generated method stub	
	}

}
