package com.cortex.base;

import com.cortex.base.config.InhibitorySynapticPlasticityConfig;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;

public class InhibitorySynapticPlasticityRule implements IPlasticityRule {
	
    private float currentWeight;
    
	private final InhibitorySynapticPlasticityConfig config;

	public InhibitorySynapticPlasticityRule(InhibitorySynapticPlasticityConfig config) {
		this.config = config;
		this.currentWeight = config.INITIAL_WEIGHT;
	}
	
	@Override
	public boolean onPostSpike(Synapse s, long dt, long now) {
		float postRate = s.getTarget().getRecentFiringRate(now);
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        this.currentWeight = Maths.clamp(this.currentWeight + dw, config.W_MIN, config.W_MAX);
        return false;
	}

	@Override
	public float getWeight() {
		return currentWeight;
	}

	@Override
	public boolean onPreSpike(long dt) {
		// inibitori NON rinforzano su pre
		return false;
	}

	@Override
	public boolean hadSignificantPairing() {
		// TODO
		return false;
	}
	
	@Override
    public void update(long now, Synapse s) {
        float postRate = s.getTarget().getRecentFiringRate(now);
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        currentWeight = Maths.clamp(currentWeight + dw, config.W_MIN, config.W_MAX);
    }

	@Override
	public boolean isEligible(long now, long window) {
		return false;
	}

	@Override
	public void applyReward(float r, long t, float neuromodulator) {}
}
