package com.cortex.base.externals;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.IProcessable;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

public abstract class ExternalModule implements IProcessable {

	private volatile boolean active = false;
	private volatile long lastProcessTime = 0;

	public abstract boolean processExt(long now) throws InterruptedException;

	public abstract ExternalConnConfig getExternalConnConfig();

	public abstract long getWaitTimeNanos();

	public abstract String getId();

	public abstract boolean isProducer();

	public abstract AbstractNeuron[][] getNeurons();

	
	public long getLastProcessTime() {
		return lastProcessTime;
	}

	public boolean isActive() {
		return active;
	}

	public void stop() {
		this.active = false;
	}

	public void start() {
		this.active = true;
	}    

	@Override
	public boolean process(long now) throws InterruptedException {
		this.lastProcessTime = now;
		if (!isActive())
			return false;
		return processExt(now);
	}

	public float randomGaussian() {
		return (float)Maths.nextGaussian();
	}

	public int getSynapsesCount() {
		int ret = 0;
		AbstractNeuron[][] n = getNeurons();
		for (int x = 0; x < n.length; x++) {
			for (int y = 0; y < n[x].length; y++) {
				if(isProducer())
					ret += n[x][y].getOutSynapsesCount();
				else
					ret += n[x][y].getInSynapsesCount();
			}
		}
		return ret;
	}

	public Point3f getPluginSite() {
		return new Point3f(0.0f, 0.0f, 0.0f);
	}
}
