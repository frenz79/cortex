package com.cortex.commons.modules;

import com.cortex.base.Neuron;

public interface IClassifier<N extends Neuron> {

	public N getClassificationResult();

	public N classify(long now) throws InterruptedException;
	
	public Neuron[][] getNeurons();
}
