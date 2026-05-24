package com.cortex.globals;

import java.util.HashMap;
import java.util.Map;

public class DiscreteAdaptiveStabilizerConfig {

	// Target globali
	public float TARGET_FIRING_LOW   = 5.0f;
	public float TARGET_FIRING_HIGH  = 20.0f;

	public float TARGET_SPARSITY_MIN = 0.85f;
	public float TARGET_SPARSITY_MAX = 0.99f;

	public float MAX_SAT_MAX_RATIO   = 0.15f;
	public float MAX_SAT_MIN_RATIO   = 0.20f;

	public float MAX_ENERGY_PER_LAYER = 120_000.0f;

	public float MAX_PLASTICITY = 10_000.0f;
	public float MIN_PLASTICITY = 50.0f;

	public float MIN_STABILITY = 0.15f;

	// --- Rate limiting ---
	public float MAX_THRESHOLD_STEP = 0.002f;
	public float MAX_LEAK_STEP = 0.005f;
	public float MAX_A_PLUS_FACTOR = 0.005f;  
	public float MAX_A_MINUS_FACTOR = 0.005f;

	// --- Hysteresis ---
	public float FIRING_HYSTERESIS = 2.0f;
	public float SPARSITY_HYSTERESIS = 0.02f;

	// --- Parametri per layer ---
	public Map<Integer, LayerAdaptiveParams> perLayer = new HashMap<>();

	public static class LayerAdaptiveParams {

		public float TARGET_FIRING_LOW;
		public float TARGET_FIRING_HIGH;

		public float TARGET_SPARSITY_MIN;
		public float TARGET_SPARSITY_MAX;

		public float MAX_ENERGY;

		public LayerAdaptiveParams(
				float TARGET_FIRING_LOW, 
				float TARGET_FIRING_HIGH, 
				float TARGET_SPARSITY_MIN,
				float TARGET_SPARSITY_MAX, 
				float MAX_ENERGY) {
			super();
			this.TARGET_FIRING_LOW = TARGET_FIRING_LOW;
			this.TARGET_FIRING_HIGH = TARGET_FIRING_HIGH;
			this.TARGET_SPARSITY_MIN = TARGET_SPARSITY_MIN;
			this.TARGET_SPARSITY_MAX = TARGET_SPARSITY_MAX;
			this.MAX_ENERGY = MAX_ENERGY;
		}
	}

	public static class Builder {
		private final DiscreteAdaptiveStabilizerConfig ret;

		public Builder() {
			this.ret = new DiscreteAdaptiveStabilizerConfig();
		}

		public Builder withTargetFiring(float TARGET_FIRING_LOW, float TARGET_FIRING_HIGH) {
			ret.TARGET_FIRING_LOW = TARGET_FIRING_LOW;
			ret.TARGET_FIRING_HIGH = TARGET_FIRING_HIGH;
			return this;
		}

		public Builder withTargetSparsity(float TARGET_SPARSITY_MIN, float TARGET_SPARSITY_MAX) {
			ret.TARGET_SPARSITY_MIN = TARGET_SPARSITY_MIN;
			ret.TARGET_SPARSITY_MAX = TARGET_SPARSITY_MAX;
			return this;
		}

		public Builder withTargetSaturation(float MAX_SAT_MIN_RATIO, float MAX_SAT_MAX_RATIO) {
			ret.MAX_SAT_MIN_RATIO = MAX_SAT_MIN_RATIO;
			ret.MAX_SAT_MAX_RATIO = MAX_SAT_MAX_RATIO;
			return this;
		}

		public Builder withTargetPlasticity(float MIN_PLASTICITY, float MAX_PLASTICITY) {
			ret.MIN_PLASTICITY = MIN_PLASTICITY;
			ret.MAX_PLASTICITY = MAX_PLASTICITY;
			return this;
		}

		public Builder withMaxEnergyPerLayer(float MAX_ENERGY_PER_LAYER) {
			ret.MAX_ENERGY_PER_LAYER = MAX_ENERGY_PER_LAYER;
			return this;
		}

		public Builder withMinStability(float MIN_STABILITY) {
			ret.MIN_STABILITY = MIN_STABILITY;
			return this;
		}

		public Builder withRateThresholdStep(float MAX_THRESHOLD_STEP) {
			ret.MAX_THRESHOLD_STEP = MAX_THRESHOLD_STEP;
			return this;
		}

		public Builder withMaxLeakStep(float MAX_LEAK_STEP) {
			ret.MAX_LEAK_STEP = MAX_LEAK_STEP;
			return this;
		}

		public Builder withMaxAPercentChange(float MAX_A_MINUS_FACTOR, float MAX_A_PLUS_FACTOR) {
			ret.MAX_A_MINUS_FACTOR = MAX_A_MINUS_FACTOR;
			ret.MAX_A_PLUS_FACTOR = MAX_A_PLUS_FACTOR;
			return this;
		}

		public Builder addLayerParams( int layerId, LayerAdaptiveParams p ) {
			ret.perLayer.put(layerId, p);
			return this;
		}

		public void validate() {
			if (ret.TARGET_FIRING_LOW < 0 || ret.TARGET_FIRING_HIGH < ret.TARGET_FIRING_LOW)
				throw new IllegalArgumentException("Invalid firing targets");

			if (ret.TARGET_SPARSITY_MIN < 0 || ret.TARGET_SPARSITY_MAX > 1)
				throw new IllegalArgumentException("Invalid sparsity targets");

			if (ret.MAX_THRESHOLD_STEP <= 0 || ret.MAX_LEAK_STEP <= 0)
				throw new IllegalArgumentException("Rate limits must be positive");
		}

		public DiscreteAdaptiveStabilizerConfig build() {
			validate();
			return ret;
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	private DiscreteAdaptiveStabilizerConfig() {
		
	}
}
