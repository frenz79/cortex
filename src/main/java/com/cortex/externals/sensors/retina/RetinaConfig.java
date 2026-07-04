package com.cortex.externals.sensors.retina;

public class RetinaConfig {

	public long SAMPLING_PERIOD_NANOS = 5_000_000; 
	public long MICROSACCADE_PERIOD_NANOS = 5_000_000; 
	public float MICROSACCADE_AMPLITUDE = 1.5f;
	public int RECEPTIVE_RADIUS = 2;
	public final int RETINA_W;
	public final int RETINA_H;
	public final int TARGET_HEMISPHERE_ID;
	public final int TARGET_LAYER_ID;
	
	RetinaConfig(int RETINA_W, int RETINA_H, int TARGET_HEMISPHERE_ID, int TARGET_LAYER_ID) {
		super();
		this.RETINA_W = RETINA_W;
		this.RETINA_H = RETINA_H;
		this.TARGET_HEMISPHERE_ID = TARGET_HEMISPHERE_ID;
		this.TARGET_LAYER_ID = TARGET_LAYER_ID;
	}

	public static Builder newBuilder(int retinaW, int retinaH, int TARGET_HEMISPHERE_ID, int TARGET_LAYER_ID) {
		return new Builder(retinaW, retinaH, TARGET_HEMISPHERE_ID, TARGET_LAYER_ID );
	}	

	public static class Builder {
		private final RetinaConfig cfg;

		public Builder(int retinaW, int retinaH, int TARGET_HEMISPHERE_ID, int TARGET_LAYER_ID) {
			this.cfg = new RetinaConfig( retinaW, retinaH, TARGET_HEMISPHERE_ID, TARGET_LAYER_ID );
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
