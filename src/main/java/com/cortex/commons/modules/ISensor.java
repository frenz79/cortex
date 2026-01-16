package com.cortex.commons.modules;

import com.cortex.base.AbstractNeuron;
import com.cortex.commons.IProcessable;

public interface ISensor extends IProcessable {

	public long getWaitTime();
	
	public String getId();
	
	public boolean isActive();
	
	public void stop();
	
	public void start();
	
	public long getLastProcessTime();
	
	public AbstractNeuron[][] getNeurons();
}
