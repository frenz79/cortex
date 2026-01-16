package com.cortex.actuators;

import com.cortex.base.AbstractNeuron;
import com.cortex.commons.IProcessable;

public interface Actuator extends IProcessable {

	public long getWaitTime();
	
	public String getId();
	
	public boolean isActive();
	
	public void stop();
	
	public void start();
	
	public long getLastProcessTime();
	
	public AbstractNeuron[][] getNeurons();
}
