package com.cortex.base;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.lateral_inhibition.ILateralInhibitionStrategy;
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
	private final int layerId;
	private final int index;
	private final Point3f position;	
	private final boolean inhibitor;
	private final int spikeSign;
	protected final Map<BranchType,ILateralInhibitionStrategy> inhibitionStrategies = new EnumMap<>(BranchType.class);
	protected final NeuronState state = new NeuronState();

	private SynapseBranch[] synapsesBranches;
	private SynapseBranch[] incomingBranches;
	private SynapseBranch[] outgoingBranches;
	
	private int inSynapsesCount = 0;
	private int outSynapsesCount = 0;
	
	public void attachSynapseBranch(SynapseBranch sb) {
		if (sb.type!=BranchType.EXTERNAL)
			throw new RuntimeException("Only external branches can be attached");
		
		synapsesBranches = attach(synapsesBranches, sb);
		
		if (sb.incoming) {
			incomingBranches = attach(incomingBranches, sb);
			inSynapsesCount += sb.synapses.length;
		} else {
			outgoingBranches = attach(outgoingBranches, sb);
			outSynapsesCount += sb.synapses.length;
		}
	}
	
	private static final SynapseBranch[] attach(SynapseBranch[] arr, SynapseBranch sb) {
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
			if (synapsesBranches[i].incoming) {
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
			if (synapsesBranches[i].incoming) {
				this.incomingBranches[inc++] = synapsesBranches[i];
			} else {
				this.outgoingBranches[out++] = synapsesBranches[i];
			}
		}
	}
	
	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);

	protected Map<BranchType, List<SynapseBranch>> groupBranchesByType() {

	    Map<BranchType, List<SynapseBranch>> map = new EnumMap<>(BranchType.class);

	    for (SynapseBranch b : incomingBranches) {
	        map.computeIfAbsent(b.type, k -> new ArrayList<>()).add(b);
	    }

	    for (SynapseBranch b : outgoingBranches) {
	        map.computeIfAbsent(b.type, k -> new ArrayList<>()).add(b);
	    }

	    return map;
	}
	/*
	// 0 = in near
	// 1 = in far
	// 2 = out near
	// 3 = out far
	// 4..(4+maxLayers) = in from layer[x]
	// (4+maxLayers)..(2*maxLayers) = out to layer[x]
	private boolean isIncomingBranch(int i) {
		if (synapsesBranchesTmp.length==1) {
			return false;
		}
		int maxLayers = (synapsesBranchesTmp.length-4)/2;
		return i<2 || (i>=4 && i<(4+maxLayers));
	}

	int branchesCount = (maxLayers<0)?1:(4 + 2*maxLayers);

	this.synapsesBranchesTmp =  (List<Synapse>[]) new List<?>[ branchesCount ];
	for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
		this.synapsesBranchesTmp[i] = new ArrayList<>();
	}
	this.synapsesBranches = new SynapseBranch[ branchesCount ];
	
	private List<Synapse>[] synapsesBranchesTmp;
	*/
	public AbstractNeuron(int layerId, int index, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position ) {
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layerId = layerId;
		/*
		inhibitionStrategies.put(
			    BranchType.NEAR,
			    new WinnerTakeMostInhibition(cfg.wtaNear)
			);

			inhibitionStrategies.put(
			    BranchType.FAR,
			    new ContinuousInhibition(cfg.continuousFar)
			);

			inhibitionStrategies.put(
			    BranchType.LAYER_FEEDFORWARD,
			    new NormalizedBranchInhibition(cfg.normalizedFF)
			);

			inhibitionStrategies.put(
			    BranchType.LAYER_FEEDBACK,
			    new ContinuousInhibition(cfg.continuousFB)
			);
			*/
	}

	public final void fire( long now ) throws InterruptedException {
		state.pendingFire = true;
	}

	public final void delayedFire( long now ) {		
		for (SynapseBranch sb : synapsesBranches) {
			if (sb.incoming) {
				for (Synapse s : sb.synapses) {
					if (s.wasFrequentlyActiveInLastWindow()) {
						s.onPostSpike(now, now);
					}
				}
			} else {
				for (Synapse s : sb.synapses) {
					long arrival = now + s.getTraversalTimeNanos(now);
					s.addSpike(Spike.createWithJitter(isInhibitor(), arrival));
				}
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
		return layerId;
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
		return "AbstractNeuron [layerId=" + layerId + ", index=" + index + ", position=" + position + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, layerId);
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
		return index == other.index && layerId == other.layerId;
	}

	public int getInSynapsesCount() {
		return inSynapsesCount;
	}

	public int getOutSynapsesCount() {
		return outSynapsesCount;
	}
}
