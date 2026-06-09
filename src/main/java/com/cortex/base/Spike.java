package com.cortex.base;

import java.util.concurrent.ThreadLocalRandom;

import com.cortex.commons.Maths;

public record Spike(
		float amplitude,
		boolean inhibitor,
		long arrivalTime) {
	
	private static final float MAX_AMPLITUDE = 200.0f;
	private static final float DEFAULT_AMPLITUDE = 100.0f;
	
	static final class JitterTable {
	    private static final int SIZE = 1024;
	    private static final long[] TABLE = new long[SIZE];

	    static {
	        ThreadLocalRandom rnd = ThreadLocalRandom.current();
	        for (int i = 0; i < SIZE; i++) {
	            TABLE[i] = rnd.nextLong(-50, 50); // ±50 ns
	        }
	    }

	    public static long next() {
	        return TABLE[ThreadLocalRandom.current().nextInt(SIZE)];
	    }
	}
	
	public Spike(float amplitude, boolean inhibitor, long arrivalTime) {
		this.amplitude = Maths.min(amplitude, MAX_AMPLITUDE);
		this.inhibitor = inhibitor;
		this.arrivalTime = arrivalTime;
	}
		
	public Spike(boolean inhibitor, long arrivalTime) {
		this(DEFAULT_AMPLITUDE,inhibitor, arrivalTime);
	}

	public static final Spike createWithJitter(float amplitude, boolean inhibitor, long arrivalTime) {
	    return new Spike(amplitude, inhibitor, arrivalTime + JitterTable.next());
	}
	
	public static final Spike createWithJitter(boolean inhibitor, long arrivalTime) {
	    return createWithJitter(DEFAULT_AMPLITUDE, inhibitor, arrivalTime );
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
