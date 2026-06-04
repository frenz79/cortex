package com.cortex.commons.modules;

import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.AbstractNeuron;
import com.cortex.commons.IProcessable;
import com.cortex.commons.Point3f;

public interface ISensor extends IProcessable {

	public long getWaitTimeNanos();
	
	public String getId();
	
	public boolean isActive();
	
	public void stop();
	
	public void start();
	
	public long getLastProcessTime();
	
	public AbstractNeuron[][] getNeurons();
	
	public static float randomGaussian() {
		return (float)ThreadLocalRandom.current().nextGaussian();
	}

	public int getSynapsesCount();
	
	public Point3f getPluginSite();
}
