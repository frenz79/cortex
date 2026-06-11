package com.cortex.base.lateralinhibition;

public class WinnerTakeMostInhibitionConfig {

    public final float WTA_INHIBITION_LEVEL;
    public final long WTA_TAU_NANOS;

    private WinnerTakeMostInhibitionConfig(Builder b) {
        this.WTA_INHIBITION_LEVEL = b.WTA_INHIBITION_LEVEL;
        this.WTA_TAU_NANOS = b.WTA_TAU_NANOS;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private float WTA_INHIBITION_LEVEL = 1.0f;
        private long WTA_TAU_NANOS = 20_000_000L;

        public Builder withLevel(float level) {
            this.WTA_INHIBITION_LEVEL = level;
            return this;
        }

        public Builder withTauNanos(long tau) {
            this.WTA_TAU_NANOS = tau;
            return this;
        }

        public WinnerTakeMostInhibitionConfig build() {
            return new WinnerTakeMostInhibitionConfig(this);
        }
    }
}

