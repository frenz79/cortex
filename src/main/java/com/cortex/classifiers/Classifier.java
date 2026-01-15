package com.cortex.classifiers;

import com.cortex.base.AbstractNeuron;

public interface Classifier<N extends AbstractNeuron> {

	public N getClassificationResult();

	void classify(long now) throws InterruptedException;
	
	public AbstractNeuron[][] getNeurons();
}
