package com.cortex.base.lateralinhibition;

public class NormalizedBranchInhibitionConfig {

	public float NORMALIZATION_STRENGTH = 1.0f;
	public float STABILITY_FACTOR = 0.1f;
	
	private NormalizedBranchInhibitionConfig(Builder b) {
        this.NORMALIZATION_STRENGTH = b.NORMALIZATION_STRENGTH;
        this.STABILITY_FACTOR = b.STABILITY_FACTOR;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private float NORMALIZATION_STRENGTH = 1.0f;
        private float STABILITY_FACTOR = 0.1f;

        public Builder withStrength(float strength) {
            this.NORMALIZATION_STRENGTH = strength;
            return this;
        }

        public Builder withStability(float stability) {
            this.STABILITY_FACTOR = stability;
            return this;
        }

        public NormalizedBranchInhibitionConfig build() {
            return new NormalizedBranchInhibitionConfig(this);
        }
    }
}
