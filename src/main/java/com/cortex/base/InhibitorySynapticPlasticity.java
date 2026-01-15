package com.cortex.base;

import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;

public class InhibitorySynapticPlasticity implements IPlasticityRule {

	private static final float learningRate = 0.0005f;
    private static final float targetRate = 5.0f; // spike per finestra
    private static final float wMin = -5.0f;
    private static final float wMax = 0.0f;
    private float weight;
    
    public InhibitorySynapticPlasticity(float initialWeight) {
        this.weight = initialWeight;
    }
    
	@Override
	public void onPostSpike(Synapse s, long dt, long now) {
		float postRate = s.getTarget().getRecentFiringRate(now);
        float error = postRate - targetRate;
        float dw = learningRate * error;
        this.weight = Maths.clamp(this.weight + dw, wMin, wMax);
	}

	@Override
	public float getWeight() {
		return weight;
	}

	@Override
	public void onPreSpike(long dt) {
		// inibitori NON rinforzano su pre
	}

}
