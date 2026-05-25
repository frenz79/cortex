package com.cortex.base.config;

import java.util.concurrent.TimeUnit;

public class CorticalNeuronsConfig implements INeuronConfig{

	public float POTENTIAL_MAX = 3.0f;
	public float POTENTIAL_MIN = -2.0f;
	public float FIRING_THRESHOLD = 0.12f;
	public float POTENTIAL_ZERO = 0.0f;
	public long  REFRACTORY_PERIOD_NANOS = TimeUnit.MILLISECONDS.toNanos(5);
	public float REPOLARIZATION_PER_SECOND  = 0.2f; // potential units per second
	public long  RATE_WINDOW = 100_000_000L; // 100 ms
	public float RATE_DECAY_PER_WINDOW = 0.95f; // per RATE_WINDOW

	public static Builder newBuilder() {
		return new Builder();
	}	

	public static class Builder {
		private final CorticalNeuronsConfig cfg;

		public Builder() {
			this.cfg = new CorticalNeuronsConfig();
		}

		public Builder withPotential(float POTENTIAL_MIN, float POTENTIAL_MAX, float POTENTIAL_ZERO) {
			cfg.POTENTIAL_MIN = POTENTIAL_MIN;
			cfg.POTENTIAL_MAX = POTENTIAL_MAX;
			cfg.POTENTIAL_ZERO = POTENTIAL_ZERO;
			return this;
		}

		public Builder withRefractoryPeriodNanos(long REFRACTORY_PERIOD_NANOS) {
			cfg.REFRACTORY_PERIOD_NANOS = REFRACTORY_PERIOD_NANOS;
			return this;
		}

		public Builder withRepolarizationPerSecond(float REPOLARIZATION_PER_SECOND) {
			cfg.REPOLARIZATION_PER_SECOND = REPOLARIZATION_PER_SECOND;
			return this;
		}

		public Builder withFiringThreshold(float FIRING_THRESHOLD) {
			cfg.FIRING_THRESHOLD = FIRING_THRESHOLD;
			return this;
		}

		public Builder withRate( long RATE_WINDOW, float RATE_DECAY_PER_WINDOW ) {		
			cfg.RATE_WINDOW = RATE_WINDOW;
			cfg.RATE_DECAY_PER_WINDOW = RATE_DECAY_PER_WINDOW;
			return this;
		}

		private void validate() {
			if (cfg.POTENTIAL_MAX <= cfg.POTENTIAL_MIN)
				throw new IllegalArgumentException("POTENTIAL_MAX must be > POTENTIAL_MIN");

			if (cfg.FIRING_THRESHOLD <= cfg.POTENTIAL_MIN || cfg.FIRING_THRESHOLD >= cfg.POTENTIAL_MAX)
				throw new IllegalArgumentException("FIRING_THRESHOLD must be between POTENTIAL_MIN and POTENTIAL_MAX");

			if (cfg.REPOLARIZATION_PER_SECOND <= 0)
				throw new IllegalArgumentException("REPOLARIZATION_PER_SECOND must be > 0");

			if (cfg.RATE_WINDOW <= 0)
				throw new IllegalArgumentException("RATE_WINDOW must be > 0");

			if (cfg.RATE_DECAY_PER_WINDOW <= 0 || cfg.RATE_DECAY_PER_WINDOW > 1)
				throw new IllegalArgumentException("RATE_DECAY_PER_WINDOW must be in (0,1]");
		}

		public CorticalNeuronsConfig build() {
			validate();
			return cfg;
		}
	}
}
