package com.cortex.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.commons.IProcessable;
import com.cortex.commons.Point3f;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuron implements IProcessable {

	protected final Logger logger = LogManager.getLogger(this.getClass());

	final List<Synapse> inSynapses;
	final List<Synapse> outSynapses;
	private final int layerId;
	private final int index;
	private final Point3f position;	
	private final boolean inhibitor;
	private volatile boolean isActive = false;
	private final int spikeSign;
	
    protected float firingRate = 0.0f;
    protected long lastRateUpdate = System.nanoTime();
    
	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);
	
	public AbstractNeuron(int layerId, int index, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position) {
		this.inSynapses = (hasIncoming)
			? new ArrayList<>():Collections.emptyList();
		this.outSynapses = (hasOutgoing)
			? new ArrayList<>():Collections.emptyList();
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layerId = layerId;
	}
	
	public void fire(List<Spike> spikes) throws InterruptedException {
		for ( Spike spike : spikes ) {
			fire( spike );
		}
	}
	
	public void fire(Spike spike) throws InterruptedException {
		for ( Synapse s : this.outSynapses  ) {
			s.addSpike(spike);
		    s.getTarget().setActive(true);
		}
		// Move out, otherwise firing rate would be affected by synapses count and not just by 
		// real activity
		firingRate += 1.0f;
		lastRateUpdate = spike.getCreationTimeNanos();
		EventBus.fire(EventType.NEURON_FIRED, lastRateUpdate, this, null);
	}
	/*	
	public void synapseUpdated( long time, Synapse synapse, float oldW, float newW) {
		GlobalContext.traceSynapseWeightUpdated(time, layerId, oldW, newW);
	}
	*/
	public int getIndex() {
		return index;
	}
	
	public boolean isInhibitor() {
		return inhibitor;
	}
	
	public int getSpikeSign() {
		return spikeSign;
	}
	
	public Point3f getPosition() {
		return position;
	}

	public void addIncomingSynapse(Synapse s) {
		try {
			synchronized(inSynapses) {
				this.inSynapses.add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add incoming synapse to:{}",this.toString());
			throw ex;
		}
	}

	public void addOutgoingSynapse(Synapse s) {
		try {
			synchronized(outSynapses) {
				this.outSynapses.add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add outgoing synapse to:{}",this.toString());
			throw ex;
		}
	}
	
	public List<Synapse> getInSynapses() {
		return inSynapses;
	}

	public int getLayerId() {
		return layerId;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public List<Synapse> getOutSynapses() {
		return outSynapses;
	}

	@Override
	public String toString() {
		return "AbstractNeuron [layerId=" + layerId + ", index=" + index + ", position=" + position + "]";
	}
}
