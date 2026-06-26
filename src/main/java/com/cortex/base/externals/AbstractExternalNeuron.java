package com.cortex.base.externals;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.utils.CompactBuffer;

public abstract class AbstractExternalNeuron extends AbstractNeuron {
	
	public AbstractExternalNeuron(
			NeuronsStateBuff neuronsStates,
			int index, 
			boolean hasIncoming, 
			boolean hasOutgoing) {
		super(neuronsStates, null, index, hasIncoming, hasOutgoing, false, null);
	}
	
    @Override
    public boolean process(long currTimeNanos) {
        return true;
    }
    
    @Override
    public int getLayerId() {
		return -1;
	}
}
