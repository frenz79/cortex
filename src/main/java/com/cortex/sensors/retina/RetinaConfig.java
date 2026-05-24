package com.cortex.sensors.retina;

public class RetinaConfig {

	public long SAMPLING_PERIOD_NANOS = 20_000_000; // 5 ms
	public long MICROSACCADE_PERIOD_NANOS = 40_000_000; // 20 ms
	public float MICROSACCADE_AMPLITUDE = 0.1f;
	public int RECEPTIVE_RADIUS = 1;
	public final int RETINA_W;
	public final int RETINA_H;

	RetinaConfig(int RETINA_W, int RETINA_H) {
		super();
		this.RETINA_W = RETINA_W;
		this.RETINA_H = RETINA_H;
	}

	public static Builder newBuilder(int retinaW, int retinaH) {
		return new Builder(retinaW, retinaH);
	}	

	public static class Builder {
		private final RetinaConfig cfg;

		public Builder(int retinaW, int retinaH) {
			this.cfg = new RetinaConfig( retinaW, retinaH);
		}

		public Builder withMicrosaccade(long MICROSACCADE_PERIOD_NANOS, float MICROSACCADE_AMPLITUDE) {
			cfg.MICROSACCADE_PERIOD_NANOS = MICROSACCADE_PERIOD_NANOS;
			cfg.MICROSACCADE_AMPLITUDE = MICROSACCADE_AMPLITUDE;
			return this;
		}

		public Builder withSamplingPeriod(long SAMPLING_PERIOD_NANOS) {
			cfg.SAMPLING_PERIOD_NANOS = SAMPLING_PERIOD_NANOS;
			return this;
		}

		public Builder withReceptiveRadius(int RECEPTIVE_RADIUS) {
			cfg.RECEPTIVE_RADIUS = RECEPTIVE_RADIUS;
			return this;
		}

		private void validate() {
			if (cfg.RETINA_W <= 0 || cfg.RETINA_H <= 0)
				throw new IllegalArgumentException("retinaW/retinaH must be > 0");

			if (cfg.RECEPTIVE_RADIUS <= 0)
				throw new IllegalArgumentException("RECEPTIVE_RADIUS must be > 0");

			if (cfg.RECEPTIVE_RADIUS >= cfg.RETINA_W || cfg.RECEPTIVE_RADIUS >= cfg.RETINA_H)
				throw new IllegalArgumentException("RECEPTIVE_RADIUS must be < RETINA_W/RETINA_H");

			if (cfg.SAMPLING_PERIOD_NANOS <= 0)
				throw new IllegalArgumentException("Sampling period must be > 0");

			if (cfg.MICROSACCADE_PERIOD_NANOS <= 0)
				throw new IllegalArgumentException("Microsaccade period must be > 0");

			if (cfg.MICROSACCADE_AMPLITUDE < 0f || cfg.MICROSACCADE_AMPLITUDE > 5f)
				throw new IllegalArgumentException("Microsaccade amplitude out of range");
		}

		public RetinaConfig build() {
			validate();
			return cfg;
		}
	}
}
