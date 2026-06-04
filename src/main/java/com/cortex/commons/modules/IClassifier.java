package com.cortex.commons.modules;

import com.cortex.base.AbstractNeuron;
import com.cortex.commons.Point3f;

public interface IClassifier<N extends AbstractNeuron> {

	public N getClassificationResult();

	public N classify(long now) throws InterruptedException;
	
	public AbstractNeuron[][] getNeurons();

	public int getSynapsesCount();
	
	public Point3f getPluginSite();
}
