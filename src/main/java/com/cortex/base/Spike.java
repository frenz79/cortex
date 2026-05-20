package com.cortex.base;

public record Spike(
		float amplitude,
		long creationTimeNanos,
		boolean inhibitor ) {
	
	private static final float MAX_AMPLITUDE = 200.0f;
	private static final float DEFAULT_AMPLITUDE = 100.0f;
	private static final float DEFAULT_SPEED = 10.0f;
	
	public Spike(float amplitude, long creationTimeNanos, boolean inhibitor) {
		this.amplitude = Math.min(amplitude, MAX_AMPLITUDE);
		this.inhibitor = inhibitor;
		this.creationTimeNanos = creationTimeNanos;
	}
		
	public Spike(long creationTimeNanos, boolean inhibitor) {
		this(DEFAULT_AMPLITUDE,creationTimeNanos,inhibitor);
	}
	
	private static float clampAmplitude(float a) {
        if (Float.isNaN(a) || a <= 0f) return 0f;
        return Math.min(a, MAX_AMPLITUDE);
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
	
	/**
     * Calcola il tempo di viaggio in nanosecondi per una distanza (same units as speed).
     * Assumiamo speed in unità/secondo; conversione a nanos effettuata qui.
     */
	public long travelTimeNanos(float length) {
	    if (length <= 0f) return 0L;
	    double microseconds = (length / getSpeed()) * 1_000.0; // 1 unità = 1 µs
	    return (long)(microseconds * 1_000L); // µs → ns
	}

	@Override
	public String toString() {
		return "Spike [ creationTimeNanos=" + creationTimeNanos + ", amplitude=" + amplitude + "]";
	}
}
