package com.cortex.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.lateralinhibition.ContinuousInhibition;
import com.cortex.base.lateralinhibition.ILateralInhibitionStrategy;
import com.cortex.base.lateralinhibition.NormalizedBranchInhibition;
import com.cortex.base.lateralinhibition.WinnerTakeMostInhibition;
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

	private final int layerId;
	private final int index;
	private final Point3f position;	
	private final boolean inhibitor;
	private final int spikeSign;
	protected final Map<BranchType,ILateralInhibitionStrategy> inhibitionStrategies = new EnumMap<>(BranchType.class);
	protected final NeuronState state = new NeuronState();

	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now);
	
	public enum BranchType {
	    NEAR,
	    FAR,
	    LAYER_FEEDFORWARD,
	    LAYER_FEEDBACK
	}
	
	public static final class SynapseBranch {
		public final Synapse[] synapses;
		public final boolean incoming;
		public final BranchType type;
		
		public SynapseBranch(Synapse[] synapses, boolean incoming, BranchType type) {
			super();
			this.synapses = synapses;
			this.incoming = incoming;
			this.type = type;
		}

		// Branch dynamic state
		public float branchPotential;      // Weighted input sum
		public float branchActivity;       // Recent activity (for decay)
		public float inhibition;           // Lateral inhibition level
		public float gain = 1.0f;          // Branch modulator
	}

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

	private List<Synapse>[] synapsesBranchesTmp;
	private SynapseBranch[] synapsesBranches;
	private SynapseBranch[] incomingBranches;
	private SynapseBranch[] outgoingBranches;

	public AbstractNeuron(int layerId, int index, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position, int maxLayers) {
		this.inhibitor = inhibitor;
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layerId = layerId;
		int branchesCount = (maxLayers<0)?1:(4 + 2*maxLayers);

		this.synapsesBranchesTmp =  (List<Synapse>[]) new List<?>[ branchesCount ];
		for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
			this.synapsesBranchesTmp[i] = new ArrayList<>();
		}
		this.synapsesBranches = new SynapseBranch[ branchesCount ];
		
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
	}

	public void compact() {

	    List<SynapseBranch> in = new ArrayList<>();
	    List<SynapseBranch> out = new ArrayList<>();

	    for (int i = 0; i < synapsesBranchesTmp.length; i++) {

	        List<Synapse> list = synapsesBranchesTmp[i];
	        Synapse[] arr = list.toArray(new Synapse[0]);

	        boolean incoming = isIncomingBranch(i);

	        if (arr.length > 32) { // TODO: from config

	            // numero di sub-branches
	            int numSplits = (int) Math.ceil(arr.length / 32.0);

	            for (int s = 0; s < numSplits; s++) {

	                int start = s * 32;
	                int end = Math.min(start + 32, arr.length);

	                Synapse[] split = Arrays.copyOfRange(arr, start, end);

	                SynapseBranch b = new SynapseBranch(split, incoming);

	                if (incoming) in.add(b);
	                else out.add(b);
	            }

	        } else {

	            SynapseBranch b = new SynapseBranch(arr, incoming);

	            if (incoming) in.add(b);
	            else out.add(b);
	        }
	    }

	    incomingBranches = in.toArray(new SynapseBranch[0]);
	    outgoingBranches = out.toArray(new SynapseBranch[0]);

	    Arrays.fill(synapsesBranchesTmp, null);
	    synapsesBranchesTmp = null;
	}

	// Used only at build time
	public void addSynapse(Synapse s, boolean incoming, boolean near) {
		try {
			int index = (incoming ? 0 : 2) + (near ? 0 : 1);
			if (synapsesBranchesTmp.length==1) {
				// Special case for sensors
				index = 0;
			}
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
			if (synapsesBranchesTmp.length==1) {
				// Special case for sensors
				index = 0;
			} else {
				int maxLayers = (synapsesBranchesTmp.length-4)/2;
				if (!incoming) index += maxLayers;
			}
			synchronized(synapsesBranchesTmp[index]) {
				synapsesBranchesTmp[index].add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add incoming:{} synapse to:{}",incoming, this.toString());
			throw ex;
		}
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

	// Called only at build time
	public boolean hasOutSynapses() {
		if (synapsesBranchesTmp!=null) {
			for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
				if (!isIncomingBranch(i) && !synapsesBranchesTmp[i].isEmpty()){
					return true;
				}
			}
		} else {
			for (SynapseBranch sb : synapsesBranches) {
				if (!sb.incoming && sb.synapses.length>0) {
					return true;
				}
			}
		}
		return false;
	}
	// Called only at build time
	public boolean hasInSynapses() {
		if (synapsesBranchesTmp!=null) {
			for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
				if (isIncomingBranch(i) && !synapsesBranchesTmp[i].isEmpty()){
					return true;
				}
			}
		} else {
			for (SynapseBranch sb : synapsesBranches) {
				if (sb.incoming && sb.synapses.length>0) {
					return true;
				}
			}
		}
		return false;
	}
	// Called only at build time
	public int getInSynapsesCount() {
		int count = 0;
		if (synapsesBranchesTmp!=null) {
			for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
				if (isIncomingBranch(i)){
					count += synapsesBranchesTmp[i].size();
				}
			}
		} else {
			for (SynapseBranch sb : synapsesBranches) {
				if (sb.incoming) {
					count += sb.synapses.length;
				}
			}
		}
		return count;
	}
	// Called only at build time
	public int getOutSynapsesCount() {
		int count = 0;
		if (synapsesBranchesTmp!=null) {
			for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
				if (!isIncomingBranch(i)){
					count += synapsesBranchesTmp[i].size();
				}
			}
		} else {
			for (SynapseBranch sb : synapsesBranches) {
				if (!sb.incoming) {
					count += sb.synapses.length;
				}
			}
		}
		return count;
	}
}
