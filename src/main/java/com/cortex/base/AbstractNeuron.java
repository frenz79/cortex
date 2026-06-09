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

	protected final NeuronState state = new NeuronState();

	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);


	public final class SynapseBranch {
		public final Synapse[] synapses;
		public SynapseBranch(Synapse[] synapses) {
			super();
			this.synapses = synapses;
		}

		// Stato dinamico del branch
		public float branchPotential;      // somma pesata degli input
		public float branchActivity;       // attività recente (decadimento)
		public float inhibition;           // livello di inibizione laterale
		public float gain = 1.0f;          // modulazione del branch

		// Metadati utili
		// public final float centerX, centerY, centerZ; // centro geometrico del branch
		// public final float radius;                    // raggio del RF
	}

	// 0 = in near
	// 1 = in far
	// 2 = out near
	// 3 = out far
	// 4..(4+maxLayers) = in from layer[x]
	// (4+maxLayers)..(2*maxLayers) = out to layer[x]
	private List<Synapse>[] synapsesBranchesTmp;
	private final SynapseBranch[] synapsesBranches;

	public AbstractNeuron(int layerId, int index, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position, int maxLayers) {
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layerId = layerId;
		this.synapsesBranchesTmp =  (List<Synapse>[]) new List<?>[ 4 + 2*maxLayers ];
		for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
			synapsesBranchesTmp[i] = new ArrayList<>();
		}
		this.synapsesBranches = new SynapseBranch[ 4 + 2*maxLayers ];
	}

	public void compact() {
		final var type = new Synapse[]{};
		for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
			this.synapsesBranches[i] = new SynapseBranch( 
					synapsesBranchesTmp[i].toArray(type));
		}
		this.synapsesBranchesTmp = null;
	}

	// Used only at build time
	public void addSynapse(Synapse s, boolean incoming, boolean near) {
		try {
			int index = 0;
			if (incoming && !near) index = 1;
			else if (!incoming && near) index = 2;
			else if (!incoming && !near) index = 3;
			
			synchronized(synapsesBranchesTmp[index]) {
				synapsesBranchesTmp[index].add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add incoming:{} synapse to:{}",incoming, this.toString());
			throw ex;
		}
	}
	// Used only at build time
	public void addSynapse(Synapse s, boolean incoming, int layer) {
		try {
			int index = 4 + layer;
			int maxLayers = (synapsesBranchesTmp.length-4)/2;
			if (!incoming) index += maxLayers;
			synchronized(synapsesBranchesTmp[index]) {
				synapsesBranchesTmp[index].add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add incoming:{} synapse to:{}",incoming, this.toString());
			throw ex;
		}
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
