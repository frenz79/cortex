package com.cortex.base.config;

public class InhibitorySynapticPlasticityConfig {

	public float LEARNING_RATE = 0.005f;
	public float TARGET_FIRING_RATE =  2.5f;
	public float W_MIN;
	public float W_MAX;
	public float INITIAL_WEIGHT;
	
	public static class Builder {
		private final InhibitorySynapticPlasticityConfig ret;

		public Builder() {
			this.ret = new InhibitorySynapticPlasticityConfig();
		}

		public Builder withLearningRate(float LEARNING_RATE) {
			if (LEARNING_RATE <= 0f) throw new IllegalArgumentException("LEARNING_RATE must be > 0");
			if (LEARNING_RATE > 1f) throw new IllegalArgumentException("LEARNING_RATE too high");
			ret.LEARNING_RATE = LEARNING_RATE;
			return this;
		}
		
		public Builder withTargetFiringRate(float TARGET_FIRING_RATE) {
			if (TARGET_FIRING_RATE < 0f) throw new IllegalArgumentException("TARGET_FIRING_RATE must be >= 0");
			if (TARGET_FIRING_RATE < 0f || TARGET_FIRING_RATE > 1000f) throw new IllegalArgumentException("TARGET_FIRING_RATE out of range");
			ret.TARGET_FIRING_RATE = TARGET_FIRING_RATE;
			return this;
		}
		
		public Builder withWeights(float INITIAL_WEIGHT, float W_MAX, float W_MIN) {
			if (W_MIN < 0f || W_MAX <= W_MIN) throw new IllegalArgumentException("Invalid weight bounds");
			if (INITIAL_WEIGHT < W_MIN || INITIAL_WEIGHT > W_MAX) throw new IllegalArgumentException("Invalid INITIAL_WEIGHT");
			ret.W_MIN = W_MIN;
			ret.W_MAX = W_MAX;
			ret.INITIAL_WEIGHT = INITIAL_WEIGHT;
			return this;
		}
		
		private void validate() {
			
		}
		
		public InhibitorySynapticPlasticityConfig build() {
			validate();
			return ret;
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}
}
