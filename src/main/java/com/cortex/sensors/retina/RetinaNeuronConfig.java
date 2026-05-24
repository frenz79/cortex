package com.cortex.sensors.retina;

public class RetinaNeuronConfig {

	public float ON_GAIN  = 0.4f;
	public float OFF_GAIN = 0.4f;
	public float THRESHOLD = 0.2f;
	public int MAX_SPIKES_PER_SAMPLE = 1;

	public static Builder newBuilder() {
		return new Builder();
	}	

	public static class Builder {
		private final RetinaNeuronConfig cfg;

		public Builder() {
			this.cfg = new RetinaNeuronConfig();
		}

		public Builder withGain(float ON_GAIN, float OFF_GAIN) {
			cfg.ON_GAIN = ON_GAIN;
			cfg.OFF_GAIN = OFF_GAIN;
			return this;
		}

		public Builder withThreshold(float THRESHOLD) {
			cfg.THRESHOLD = THRESHOLD;
			return this;
		}

		public Builder withMaxSpikesPerSample(int MAX_SPIKES_PER_SAMPLE) {
			cfg.MAX_SPIKES_PER_SAMPLE = MAX_SPIKES_PER_SAMPLE;
			return this;
		}

		private void validate() {
			if (cfg.ON_GAIN <= 0)
				throw new IllegalArgumentException("ON_GAIN must be > 0");
			if (cfg.OFF_GAIN <= 0)
				throw new IllegalArgumentException("OFF_GAIN must be > 0");
			if (cfg.THRESHOLD < 0)
				throw new IllegalArgumentException("THRESHOLD must be >= 0");
			if (cfg.MAX_SPIKES_PER_SAMPLE <= 0)
				throw new IllegalArgumentException("MAX_SPIKES_PER_SAMPLE must be > 0");
		}

		public RetinaNeuronConfig build() {
			validate();
			return cfg;
		}
	}
}
