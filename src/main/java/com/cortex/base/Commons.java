package com.cortex.base;

import java.util.function.Predicate;

public interface Commons {
	
	public static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	public static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	public static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = AbstractNeuron::isInhibitor;

}
