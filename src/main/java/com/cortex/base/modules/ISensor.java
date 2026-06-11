package com.cortex.base.modules;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.IProcessable;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

public interface ISensor extends IProcessable {

	public long getWaitTimeNanos();
	
	public String getId();
	
	public boolean isActive();
	
	public void stop();
	
	public void start();
	
	public long getLastProcessTime();
	
	public AbstractNeuron[][] getNeurons();
	
	public static float randomGaussian() {
		return (float)Maths.nextGaussian();
	}

	public int getSynapsesCount();
	
	public Point3f getPluginSite();
}
