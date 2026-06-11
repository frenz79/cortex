package com.cortex.base.plasticity;

import java.util.concurrent.TimeUnit;

public class ExcitatorySynapticPlasticityConfig {

	public float A_PLUS;	
	public float A_MINUS;	
	public long TAU_PLUS;
	public long TAU_MINUS;	

	public float W_MIN;
	public float W_MAX;	
	public float W_BASELINE;

	public long ELIGIBILITY_DECAY_NANOS;	
	public float HOMEOSTATIC_RATE = 0.001f;	
	public boolean PLASTIC_DELAY = false;	
	public float PLASTIC_DELAY_MIN;	
	public float PLASTIC_DELAY_MAX;
	
	public float INITIAL_WEIGHT;
	
	public static class Builder {
		private final ExcitatorySynapticPlasticityConfig ret;

		public Builder() {
			this.ret = new ExcitatorySynapticPlasticityConfig();
		}

		public Builder withSTDP(float A_PLUS, float A_MINUS, long TAU_PLUS, long TAU_MINUS) {
			if (A_PLUS < 0f || A_MINUS < 0f) throw new IllegalArgumentException("Invalid STDP parameters");
			if (TAU_PLUS <= 0L || TAU_MINUS <= 0L) throw new IllegalArgumentException("TAU must be > 0 (nanos)");
			ret.A_PLUS = A_PLUS;
			ret.A_MINUS = A_MINUS;
			ret.TAU_PLUS = TAU_PLUS;
			ret.TAU_MINUS = TAU_MINUS;
			return this;
		}

		public Builder withWeights(float INITIAL_WEIGHT, float W_MAX, float W_MIN, float W_BASELINE) {
		    if (W_MIN < 0f || W_MAX <= W_MIN) throw new IllegalArgumentException("Invalid weight bounds");
		    if (W_BASELINE < W_MIN || W_BASELINE > W_MAX) throw new IllegalArgumentException("Invalid W_BASELINE");
		    if (INITIAL_WEIGHT < W_MIN || INITIAL_WEIGHT > W_MAX) throw new IllegalArgumentException("Invalid INITIAL_WEIGHT");
		    ret.W_MIN = W_MIN;
			ret.W_MAX = W_MAX;
			ret.W_BASELINE = W_BASELINE;
			ret.INITIAL_WEIGHT = INITIAL_WEIGHT;
			return this;
		}

		public Builder withEligibilityDecaySeconds(float ELIGIBILITY_DECAY) {
			if (ELIGIBILITY_DECAY <= 0f || ELIGIBILITY_DECAY > 1f) throw new IllegalArgumentException("ELIGIBILITY_DECAY must be in (0,1]");
			ret.ELIGIBILITY_DECAY_NANOS = (long)(TimeUnit.SECONDS.toNanos(1) * ELIGIBILITY_DECAY);
			return this;
		}

		public Builder withPlasticity(float PLASTIC_DELAY_MAX, float PLASTIC_DELAY_MIN) {
			if (PLASTIC_DELAY_MIN < 0 || PLASTIC_DELAY_MAX <= PLASTIC_DELAY_MIN)
			    throw new IllegalArgumentException("Invalid delay bounds");
			ret.PLASTIC_DELAY = true;
			ret.PLASTIC_DELAY_MAX = PLASTIC_DELAY_MAX;
			ret.PLASTIC_DELAY_MIN = PLASTIC_DELAY_MIN;
			return this;
		}

		public Builder withHomeostaticRate(float HOMEOSTATIC_RATE) {
			ret.HOMEOSTATIC_RATE = HOMEOSTATIC_RATE;
			return this;
		}
		
		public ExcitatorySynapticPlasticityConfig build() {
			return ret;
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}
}
