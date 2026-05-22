package com.cortex.commons.modules;

import com.cortex.brain.CorticalNeuron;
import com.cortex.commons.IProcessable;

public interface IActuator extends IProcessable {

	public long getWaitTime();
	
	public String getId();
	
	public boolean isActive();
	
	public void stop();
	
	public void start();
	
	public long getLastProcessTime();
	
	public CorticalNeuron[][] getNeurons();
}
