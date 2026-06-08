package com.cortex.base;

import java.util.concurrent.ThreadLocalRandom;

import com.cortex.commons.Maths;

public record Spike(
		float amplitude,
		boolean inhibitor,
		long arrivalTime) {
	
	private static final float MAX_AMPLITUDE = 200.0f;
	private static final float DEFAULT_AMPLITUDE = 100.0f;
	
	public Spike(float amplitude, boolean inhibitor, long arrivalTime) {
		this.amplitude = Maths.min(amplitude, MAX_AMPLITUDE);
		this.inhibitor = inhibitor;
		this.arrivalTime = arrivalTime;
	}
		
	public Spike(boolean inhibitor, long arrivalTime) {
		this(DEFAULT_AMPLITUDE,inhibitor, arrivalTime);
	}

	public static Spike createWithJitter(float amplitude, boolean inhibitor, long arrivalTime) {
	    long jitter = ThreadLocalRandom.current().nextLong(-50, 50); // ±50 ns
	    return new Spike(amplitude, inhibitor, arrivalTime + jitter);
	}
	
	public static Spike createWithJitter(boolean inhibitor, long arrivalTime) {
	    long jitter = ThreadLocalRandom.current().nextLong(-50, 50); // ±50 ns
	    return new Spike(DEFAULT_AMPLITUDE, inhibitor, arrivalTime + jitter);
	}
	
	public int getSign() {
		return (inhibitor)?-1:1;
	}

	public float signedAmplitude() {
	    return amplitude * (inhibitor ? -1f : 1f);
	}
	
	@Override
	public String toString() {
		return "Spike [ arrivalTime=" + arrivalTime + ", amplitude=" + amplitude + "]";
	}
}
