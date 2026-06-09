package com.cortex.base;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.commons.IProcessable;
import com.cortex.commons.Point3f;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuron implements IProcessable {

	static final Logger logger = LogManager.getLogger(AbstractNeuron.class);

	// Hot fields grouped together for better locality
	public final class NeuronState {
		public float firingRate = 0.0f;
	    public long lastRateUpdate = System.nanoTime();
	    public boolean isActive = false;
	}
	
	private final int layerId;
	private final int index;
	private final Point3f position;	
	private final boolean inhibitor;
	private final int spikeSign;
	private final ArrayList<Synapse> inSynapses;
	private final ArrayList<Synapse> outSynapses;
	
	protected final NeuronState state = new NeuronState();
	
	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);

	public AbstractNeuron(int layerId, int index, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position) {
		this.inSynapses = (hasIncoming)
				? new ArrayList<>():null;
		this.outSynapses = (hasOutgoing)
				? new ArrayList<>():null;
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layerId = layerId;
	}

	public void compact() {
		inSynapses.trimToSize();
		outSynapses.trimToSize();
	}

	public final void fire( long now, boolean inhibitor ) throws InterruptedException {
		for ( Synapse s : this.outSynapses  ) {
			long arrival = now + s.getTraversalTimeNanos(now);
			s.addSpike(	Spike.createWithJitter(isInhibitor(), arrival));
		}
		// Move out, otherwise firing rate would be affected by synapses count and not just by 
		// real activity
		state.firingRate += 1.0f;
		state.lastRateUpdate = now;
		EventBus.fire(EventType.NEURON_FIRED, state.lastRateUpdate, this, null);
	}

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

	// Used only at build time
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

	// Used only at build time
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

	public final List<Synapse> getInSynapses() {
		return inSynapses;
	}

	public int getLayerId() {
		return layerId;
	}

	public boolean isActive() {
		return state.isActive;
	}

	public void setActive(boolean isActive) {
		state.isActive = isActive;
	}

	public final List<Synapse> getOutSynapses() {
		return outSynapses;
	}

	@Override
	public String toString() {
		return "AbstractNeuron [layerId=" + layerId + ", index=" + index + ", position=" + position + "]";
	}
}
