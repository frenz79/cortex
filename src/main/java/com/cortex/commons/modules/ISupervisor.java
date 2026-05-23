package com.cortex.commons.modules;

import com.cortex.base.AbstractNeuron;

public interface ISupervisor<N extends AbstractNeuron> {

	public void process(long now);
	public void setExpected(N expected);
	
	public IClassifier<N> getClassifier();
}
