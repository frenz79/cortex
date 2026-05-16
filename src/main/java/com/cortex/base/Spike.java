package com.cortex.base;

public record Spike(
		float amplitude,
		long creationTimeNanos,
		boolean inhibitor
	
		) {
	
	private static final float MAX_AMPLITUDE = 200.0f;
	private static final float DEFAULT_AMPLITUDE = 100.0f;
	private static final float DEFAULT_SPEED = 100.0f;
	
	public Spike(float amplitude, long creationTimeNanos, boolean inhibitor) {
		this.amplitude = Math.min(amplitude, MAX_AMPLITUDE);
		this.inhibitor = inhibitor;
		this.creationTimeNanos = creationTimeNanos;
	}
		
	public Spike(long creationTimeNanos, boolean inhibitor) {
		this(DEFAULT_AMPLITUDE,creationTimeNanos,inhibitor);
	}
	
	public long getCreationTimeNanos() {
		return creationTimeNanos;
	}

	public float getSpeed() {
		return DEFAULT_SPEED;
	}
	
	public int getSign() {
		return (inhibitor)?-1:1;
	}

	public float getAmplitude() {
		return amplitude;
	}

	@Override
	public String toString() {
		return "Spike [ creationTimeNanos=" + creationTimeNanos + ", amplitude=" + amplitude + "]";
	}
}
