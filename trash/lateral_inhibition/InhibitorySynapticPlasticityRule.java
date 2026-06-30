package com.cortex.base.plasticity;

import com.cortex.base.Synapse;
import com.cortex.base.utils.Maths;

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
    public float update(long now, Synapse s) {
        float postRate = s.getTarget().getRecentFiringRate(now);
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        currentWeight = Maths.clamp(currentWeight + dw, config.W_MIN, config.W_MAX);
        return currentWeight;
    }

	@Override
	public boolean isEligible(long now, long window) {
		return false;
	}

	@Override
	public void applyReward(float r, long t, float neuromodulator) {}
}
