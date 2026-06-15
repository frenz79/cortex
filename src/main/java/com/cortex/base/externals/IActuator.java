package com.cortex.base.externals;

import com.cortex.brain.CorticalNeuron;

public interface IActuator {

	public long getWaitTime();
	
	public String getId();
	
	public boolean isActive();
	
	public void stop();
	
	public void start();
	
	public long getLastProcessTime();
	
	public CorticalNeuron[][] getNeurons();
}
