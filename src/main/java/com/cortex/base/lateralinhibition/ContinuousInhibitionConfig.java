package com.cortex.base.lateralinhibition;

public class ContinuousInhibitionConfig {

	public long INHIBITION_TAU_NANOS = 50_000_000;
	public float INHIBITION_MAX = 1.0f;
	
	private ContinuousInhibitionConfig(Builder b) {
        this.INHIBITION_TAU_NANOS = b.INHIBITION_TAU_NANOS;
        this.INHIBITION_MAX = b.INHIBITION_MAX;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long INHIBITION_TAU_NANOS = 50_000_000L;
        private float INHIBITION_MAX = 1.0f;

        public Builder withTauNanos(long tau) {
            this.INHIBITION_TAU_NANOS = tau;
            return this;
        }

        public Builder withMax(float max) {
            this.INHIBITION_MAX = max;
            return this;
        }

        public ContinuousInhibitionConfig build() {
            return new ContinuousInhibitionConfig(this);
        }
    }
}
