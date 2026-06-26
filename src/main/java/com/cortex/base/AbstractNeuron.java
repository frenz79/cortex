package com.cortex.base;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.soa.NeuronStateSoA;
import com.cortex.base.soa.SynapseBranchStateSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.HemisphereContext;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuron implements IProcessable {

	static final Logger logger = LogManager.getLogger(AbstractNeuron.class);

	// Immutable fields
	private final Abstract3DLayer layer;
	private final int index;
	private final int hemisphereId;
	private final Point3f position;	
	private final boolean inhibitor;
	private final int spikeSign;
	
	//	private SynapseBranch[] synapsesBranches = new SynapseBranch[0];
	//	private SynapseBranch[] incomingBranches = new SynapseBranch[0];
	//	private SynapseBranch[] outgoingBranches = new SynapseBranch[0];

	private int[] allBranchIndices      = new int[0];
	private int[] incomingBranchIndices = new int[0];
	private int[] outgoingBranchIndices = new int[0];

	private int inSynapsesCount = 0;
	private int outSynapsesCount = 0;

	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);

	public synchronized void addIncomingBranch(int branchIndex, SynapseBranchStateSoA branchStateBuff) {
		addSynapseBranches(new int[]{branchIndex}, true, branchStateBuff);
	}

	public synchronized void addOutgoingBranch(int branchIndex, SynapseBranchStateSoA branchStateBuff) {
		addSynapseBranches(new int[]{branchIndex}, false, branchStateBuff);
	}

	private void addSynapseBranches(int[] branchIndices,
			boolean addToIncoming,
			SynapseBranchStateSoA branchStateBuff) {

		int synCount = 0;
		for (int b : branchIndices) {
			synCount += branchStateBuff.synapseCount[b];
		}

		this.allBranchIndices = append(this.allBranchIndices, branchIndices);
		if (addToIncoming) {
			this.incomingBranchIndices = append(this.incomingBranchIndices, branchIndices);
			this.inSynapsesCount += synCount;
		} else {
			this.outgoingBranchIndices = append(this.outgoingBranchIndices, branchIndices);
			this.outSynapsesCount += synCount;
		}
	}

	private static int[] append(int[] arr, int[] extra) {
		int arrLen = arr.length;
		int len = extra.length;
		int[] out = new int[arrLen + len];
		System.arraycopy(arr, 0, out, 0, arrLen);
		System.arraycopy(extra, 0, out, arrLen, len);
		return out;
	}

	public AbstractNeuron(Abstract3DLayer layer, int index, int hemisphereId, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position ) {
		this.index = index;
		this.hemisphereId = hemisphereId;
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.layer = layer;
	}

	public final void fire( long now ) throws InterruptedException {
		getHemisphereCtx().neuronState.pendingFire[index] = true;
		if (layer.getConfig().COMBINED_LATERAL_INHIBITION != null) {
			layer.getConfig().COMBINED_LATERAL_INHIBITION.updateInhibition(now, this, /*not used*/-0.0f);
		}
	}

	public void delayedFire( long now ) {
		// Debug log
		//	if (layer!=null && layer.getLayerId() == 0) {
		//	    logger.info("L0 {} fired, outgoing synapses count = {}", getIndex(), getOutSynapsesCount());
		//	}
		/*
		for (SynapseBranch sb : incomingBranches) {
			for (Synapse s : sb.synapses) {
				if (s.wasFrequentlyActiveInLastWindow()) {
					s.onPostSpike(now, now);
				}
			}
		}*/
		
		SynapseBranchStateSoA branchState = getHemisphereCtx().branchState;
		NeuronStateSoA neuronState = getHemisphereCtx().neuronState;
		
		for (int b : incomingBranchIndices) {
		    int start = branchState.synapseStart[b];
		    int count = branchState.synapseCount[b];
		    for (int i = start; i < start + count; i++) {
		        int synId = synapseTopology.synapseIndex[i];
		        if (synapseState.wasFrequentlyActiveInLastWindow[synId]) {
		            synapseLogic.onPostSpike(synId, now);
		        }
		    }
		}
		
		for (SynapseBranch sb : outgoingBranches) {
			for (Synapse s : sb.synapses) {
				long arrival = now + s.getTraversalTimeNanos(now);
				s.addSpike(now, Spike.createWithJitter(isInhibitor(), arrival));
				sb.active = true;
			}
		}		
		// Move out, otherwise firing rate would be affected by synapses count and not just by 
		// real activity
		neuronState.pendingFire[index] = false;
		neuronState.firingRate[index] += 1.0f;
		neuronState.lastRateUpdate[index] = now;
		EventBus.fire(EventType.NEURON_FIRED, now, this, null);
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

	public int getLayerId() {
		return layer.getLayerId();
	}

	public Abstract3DLayer getLayer() {
		return layer;
	}

	public boolean isActive() {
		return getHemisphereCtx().neuronState.isActive[index];
	}

	public boolean isPendingFire() {
		return getHemisphereCtx().neuronState.pendingFire[index];
	}

	public void setActive(boolean isActive) {
		getHemisphereCtx().neuronState.isActive[index] = isActive;
	}

	public final int[] getAllSynapseBranchIndices() {
	    return allBranchIndices;
	}

	public final int[] getInSynapseBranchIndices() {
	    return incomingBranchIndices;
	}

	public final int[] getOutSynapseBranchIndices() {
	    return outgoingBranchIndices;
	}

	@Override
	public String toString() {
		return "AbstractNeuron [layerId=" + getLayerId() + ", index=" + index + ", position=" + position + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, (layer!=null)?layer.getLayerId():-1);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractNeuron other = (AbstractNeuron) obj;
		return index == other.index && getLayerId() == other.getLayerId();
	}

	public int getInSynapsesCount() {
		return inSynapsesCount;
	}

	public int getOutSynapsesCount() {
		return outSynapsesCount;
	}
	
	public HemisphereContext getHemisphereCtx() {
		return HemisphereContext.get(hemisphereId);
	}
}
