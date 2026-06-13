package com.cortex.base;

import java.util.function.Predicate;

public interface Commons {

	public static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	public static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	public static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = AbstractNeuron::isInhibitor;

	// baseSpeed = nanoseconds per unit distance
	// FAST  ≈ 2.5 ms per unit
	// MID   ≈ 3.0 ms per unit
	// SLOW  ≈ 4.0 ms per unit
	// delay = length * baseSpeed / (1 + myelinFactor)
	public static enum SYNAPSE_SPEED {
		FAST(2_500_000l),
		MID (3_000_000l),
		SLOW(4_000_000l);

		private final long baseSpeed;

		SYNAPSE_SPEED(long l) {
			this.baseSpeed = l;
		}

		public long getBaseSpeed() {
			return baseSpeed;
		}
	}
}
