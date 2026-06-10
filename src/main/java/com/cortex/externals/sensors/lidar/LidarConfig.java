package com.cortex.externals.sensors.lidar;

public class LidarConfig {

    public int RAYS = 16;
    public float FOV_DEG = 120f;
    public float MAX_DISTANCE = 10f;

    public long SAMPLING_PERIOD_NANOS = 5_000_000; // 5ms

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final LidarConfig cfg = new LidarConfig();

        public Builder withRays(int rays) {
            cfg.RAYS = rays;
            return this;
        }

        public Builder withFov(float fovDeg) {
            cfg.FOV_DEG = fovDeg;
            return this;
        }

        public Builder withMaxDistance(float d) {
            cfg.MAX_DISTANCE = d;
            return this;
        }

        public Builder withSamplingPeriod(long nanos) {
            cfg.SAMPLING_PERIOD_NANOS = nanos;
            return this;
        }

        public LidarConfig build() {
            if (cfg.RAYS <= 0) throw new IllegalArgumentException("RAYS > 0");
            if (cfg.MAX_DISTANCE <= 0) throw new IllegalArgumentException("MAX_DISTANCE > 0");
            return cfg;
        }
    }
}
