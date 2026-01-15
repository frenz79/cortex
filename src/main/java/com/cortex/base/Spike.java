package com.cortex.base;

public class Spike {
	
	public static final float MAX_AMPLITUDE = 200.0f;
	public static final float DEFAULT_AMPLITUDE = 100.0f;
	public static final float DEFAULT_SPEED = 100.0f;
	
	private final boolean inhibitor;
	private final long creationTimeNanos;
	private final float amplitude;
        
	public Spike(float amplitude, long time, boolean inhibitor) {
		this.amplitude = Math.min(amplitude, MAX_AMPLITUDE);
		this.inhibitor = inhibitor;
		this.creationTimeNanos = time;
	}
	
	public Spike(long time, boolean inhibitor) {
		this(DEFAULT_AMPLITUDE,time,inhibitor);
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
}
