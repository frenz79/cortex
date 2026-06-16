package com.cortex.base;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.utils.Point3f;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuron implements IProcessable {

	static final Logger logger = LogManager.getLogger(AbstractNeuron.class);

	// Hot fields grouped together for better locality
	public final class NeuronState {
		public float firingRate = 0.0f;
		public long lastRateUpdate = System.nanoTime();
		public boolean isActive = false; // Phase 1
		public boolean pendingFire = false; // Phase 2
	}

	// Immutable fields
	private final Abstract3DLayer layer;
	private final int index;
	private final Point3f position;	
	private final boolean inhibitor;
	private final int spikeSign;
	protected final NeuronState state = new NeuronState();

	private SynapseBranch[] synapsesBranches = new SynapseBranch[0];
	private SynapseBranch[] incomingBranches = new SynapseBranch[0];
	private SynapseBranch[] outgoingBranches = new SynapseBranch[0];
	
	private int inSynapsesCount = 0;
	private int outSynapsesCount = 0;
	
	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);
		
	public synchronized void addIncomingBranch( SynapseBranch sb ) {
		addSynapseBranches( new SynapseBranch[] {sb}, true );
	}
	public synchronized void addOutgoingBranch( SynapseBranch sb ) {
		addSynapseBranches( new SynapseBranch[] {sb}, false );
	}
	
	private final void addSynapseBranches( SynapseBranch[] sb, boolean addToIncoming ) {
		SynapseBranch[] newSb = sb;
		this.synapsesBranches = append(this.synapsesBranches, sb);
		if (addToIncoming) {
			this.incomingBranches = append(this.incomingBranches, newSb);
			this.inSynapsesCount += newSb.length;
		} else {
			this.outgoingBranches = append(this.outgoingBranches, newSb);
			this.outSynapsesCount += newSb.length;
		}		
	}
	
	private static final SynapseBranch[] append(SynapseBranch[] arr, SynapseBranch[] sb) {
	    int len = sb.length;
	    int arrLen = arr.length;
	    SynapseBranch[] arrNew = new SynapseBranch[arrLen + len];
	    System.arraycopy(arr, 0, arrNew, 0, arrLen);
	    System.arraycopy(sb, 0, arrNew, arrLen, len);
	    return arrNew;
	}

	public AbstractNeuron(Abstract3DLayer layer, int index, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position ) {
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layer = layer;
	}

	public final void fire( long now ) throws InterruptedException {
		state.pendingFire = true;
		// Debug log
	    // if (layer.getLayerId() == 0) { // L0
	    //   logger.info("-->L0 neuron fired: {} at {}",index, now);
	    //}
	    
		if (layer.getConfig().COMBINED_LATERAL_INHIBITION != null) {
		    layer.getConfig().COMBINED_LATERAL_INHIBITION.updateInhibition(now, this, /*not used*/-0.0f);
		}
	}

	public void delayedFire( long now ) {	
		// Debug log
		//	if (layer!=null && layer.getLayerId() == 0) {
		//	    logger.info("L0 {} fired, outgoing synapses count = {}", getIndex(), getOutSynapsesCount());
		//	}
		for (SynapseBranch sb : incomingBranches) {
			for (Synapse s : sb.synapses) {
				if (s.wasFrequentlyActiveInLastWindow()) {
					s.onPostSpike(now, now);
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
	    state.pendingFire = false;
	    state.firingRate += 1.0f;
	    state.lastRateUpdate = now;
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
		return state.isActive;
	}

	public boolean isPendingFire() {
		return state.pendingFire;
	}

	public void setActive(boolean isActive) {
		state.isActive = isActive;
	}

	public final SynapseBranch[] getAllSynapseBranches() {
		return synapsesBranches;
	}

	public final SynapseBranch[] getInSynapseBranches() {
		return incomingBranches;
	}

	public final SynapseBranch[] getOutSynapseBranches() {
		return outgoingBranches;
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
}

/*
public void attachSynapseBranch(List<SynapseBranch> sbList) {
	for ( SynapseBranch sb : sbList ) {
		if (sb.type!=BranchType.EXTERNAL)
			throw new RuntimeException("Only external branches can be attached");
		
		this.synapsesBranches = attach(this.synapsesBranches, sb);
		
		if (sb.incoming) {
			this.incomingBranches = attach(this.incomingBranches, sb);
			this.inSynapsesCount += sb.synapses.length;
		} else {
			this.outgoingBranches = attach(this.outgoingBranches, sb);
			this.outSynapsesCount+= sb.synapses.length;
		}
	}
}


private static final SynapseBranch[] append(SynapseBranch[] arr, SynapseBranch sb) {
	SynapseBranch[] arrNew =  new SynapseBranch[arr.length+1];
	System.arraycopy(arr, 0, arrNew, 0, arr.length);
	arrNew[arr.length] = sb;
	return arrNew;
}

public void fillSynapseBranches( List<SynapseBranch> sb ) {
	this.synapsesBranches = new SynapseBranch[sb.size()];
	System.arraycopy(sb.toArray(new SynapseBranch[sb.size()]), 0, synapsesBranches, 0, sb.size());
	int inc = 0;
	int out = 0;
	for (int i=0; i<synapsesBranches.length; i++) {
		if ( isIncoming(overrideIncoming,override)) {
			inc++;
			inSynapsesCount += synapsesBranches[i].synapses.length;
		} else {
			out++;
			outSynapsesCount += synapsesBranches[i].synapses.length;
		}
	}
	this.incomingBranches = new SynapseBranch[inc];
	this.outgoingBranches = new SynapseBranch[out];
	inc = 0;
	out = 0;
	for (int i=0; i<synapsesBranches.length; i++) {
		if (isIncoming(synapsesBranches[i].incoming,overrideIncoming,override)) {
			this.incomingBranches[inc++] = synapsesBranches[i];
		} else {
			this.outgoingBranches[out++] = synapsesBranches[i];
		}
	}
}


private boolean isIncoming( boolean value, boolean overrideIncoming, boolean override ) {
	return (overrideIncoming)?override:value;
}
*/
