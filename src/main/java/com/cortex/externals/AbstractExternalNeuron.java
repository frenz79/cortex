package com.cortex.externals;

import com.cortex.base.AbstractNeuron;

public abstract class AbstractExternalNeuron extends AbstractNeuron {

	public AbstractExternalNeuron(
			int index, 
			boolean hasIncoming, 
			boolean hasOutgoing) {
		super(null, index, hasIncoming, hasOutgoing, false, null);
	}
	
    @Override
    public boolean process(long currTimeNanos) {
        return true;
    }
	
}
