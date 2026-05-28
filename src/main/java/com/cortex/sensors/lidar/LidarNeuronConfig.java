package com.cortex.sensors.lidar;
public class LidarNeuronConfig {

    // decadimento firing rate
    public float TAU = 1.0f;

    // guadagni dinamica (movimento relativo)
    public float APPROACH_GAIN = 1.5f; // oggetti che si avvicinano
    public float RECEDE_GAIN   = 1.0f; // oggetti che si allontanano

    // soglia variazione distanza
    public float THRESHOLD = 0.01f;

    // limite spike per campione
    public int MAX_SPIKES_PER_SAMPLE = 3;

    // scaling generale spike
    public float SPIKE_SCALING = 0.5f;

    // --- OPTIONAL: canale assoluto (proximity)
    public boolean ENABLE_PROXIMITY = true;
    public float PROXIMITY_GAIN = 1.0f;
    public float PROXIMITY_THRESHOLD = 0.1f;

    public static Builder newBuilder() {
        return new Builder();
    }

    // --------------------------------------------------------

    public static class Builder {

        private final LidarNeuronConfig cfg;

        public Builder() {
            this.cfg = new LidarNeuronConfig();
        }

        public Builder withTau(float tau) {
            cfg.TAU = tau;
            return this;
        }

        public Builder withGains(float approach, float recede) {
            cfg.APPROACH_GAIN = approach;
            cfg.RECEDE_GAIN = recede;
            return this;
        }

        public Builder withThreshold(float threshold) {
            cfg.THRESHOLD = threshold;
            return this;
        }

        public Builder withSpikeScaling(float scaling) {
            cfg.SPIKE_SCALING = scaling;
            return this;
        }

        public Builder withMaxSpikesPerSample(int max) {
            cfg.MAX_SPIKES_PER_SAMPLE = max;
            return this;
        }

        public Builder enableProximity(boolean enable) {
            cfg.ENABLE_PROXIMITY = enable;
            return this;
        }

        public Builder withProximity(float gain, float threshold) {
            cfg.PROXIMITY_GAIN = gain;
            cfg.PROXIMITY_THRESHOLD = threshold;
            cfg.ENABLE_PROXIMITY = true;
            return this;
        }

        private void validate() {

            if (cfg.APPROACH_GAIN <= 0)
                throw new IllegalArgumentException("APPROACH_GAIN must be > 0");

            if (cfg.RECEDE_GAIN <= 0)
                throw new IllegalArgumentException("RECEDE_GAIN must be > 0");

            if (cfg.THRESHOLD < 0)
                throw new IllegalArgumentException("THRESHOLD must be >= 0");

            if (cfg.MAX_SPIKES_PER_SAMPLE <= 0)
                throw new IllegalArgumentException("MAX_SPIKES_PER_SAMPLE must be > 0");

            if (cfg.SPIKE_SCALING <= 0)
                throw new IllegalArgumentException("SPIKE_SCALING must be > 0");

            if (cfg.PROXIMITY_GAIN < 0)
                throw new IllegalArgumentException("PROXIMITY_GAIN must be >= 0");

            if (cfg.PROXIMITY_THRESHOLD < 0)
                throw new IllegalArgumentException("PROXIMITY_THRESHOLD must be >= 0");
        }

        public LidarNeuronConfig build() {
            validate();
            return cfg;
        }
    }
}
