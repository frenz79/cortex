package com.cortex.base;

import java.util.function.Predicate;

public interface Commons {

	public static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	public static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	public static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = AbstractNeuron::isInhibitor;

	// baseSpeed = delay-per-unit (ns per unit length)
	// delay = length * baseSpeed / (1 + myelinFactor)
	// FAST  ≈ 2.5 ms per unit
	// MID   ≈ 3.0 ms per unit
	// SLOW  ≈ 4.0 ms per unit
	// delay = length * baseSpeed / (1 + myelinFactor)
	public static enum SYNAPSE_SPEED {
		FAST(50_000l),
		MID (75_000l),
		SLOW(100_000l);

		private final long baseSpeed;

		SYNAPSE_SPEED(long l) {
			this.baseSpeed = l;
		}

		public long getBaseSpeed() {
			return baseSpeed;
		}
	}
}
