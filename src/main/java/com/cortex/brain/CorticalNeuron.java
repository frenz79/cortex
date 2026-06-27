package com.cortex.brain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.SynapseBranch;
import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.dendritic_competition.IDendriticCompetitionStrategy;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

/**
 *  Event-driven, analog-spike, delayed, plastic Neuron
 * 
 * */
public class CorticalNeuron extends AbstractNeuron {

	private long lastProcessTime = System.nanoTime();
	private long lastDendriticCompetitionProcessTime = System.nanoTime();
	private long lastSpikeTime = 0l;
	private final CorticalNeuronsConfig config;
	private final Map<BranchType, List<SynapseBranch>> branchTypes = new EnumMap<>(BranchType.class);
	
	public CorticalNeuron(int index, int hemisphereId, Abstract3DLayer layer, boolean hasIncoming, boolean hasOutgoing, CorticalNeuronsConfig neuronsConfig, boolean inhibitor, Point3f position) {
		super(layer, index, hemisphereId, hasIncoming, hasOutgoing,	inhibitor, position	);
		this.config = neuronsConfig;
	}
	
	protected Map<BranchType, List<SynapseBranch>> groupBranchesByType() {
	    if (!branchTypes.isEmpty()) {
	    	// memoize and reuse it..
	    	// Once engine has been started branches cannot change.
	    	return branchTypes;
	    }
	    // Only incoming branches take part in dendritic competition
	    for (SynapseBranch b : getInSynapseBranches()) {
	        branchTypes.computeIfAbsent(b.type, k -> new ArrayList<>()).add(b);
	    }
	    return branchTypes;
	}
	
	@Override
	public boolean process(long now) throws InterruptedException {
		long deltaTimeNanos = now - lastProcessTime;

		// stayActive flag marks neurons that still have spikes to be processed
		boolean stayActive = false;
		float somaPotential = 0f;

		// 1. Process incoming synapses grouped by branch
		for (SynapseBranch synapseBranch : getInSynapseBranches()) {
			 
			// 1. Pre-scan
		    boolean branchHasSpikes = synapseBranch.hasSpikes();
		   		    
	    	// Reset branch potential for this tick
	        synapseBranch.branchPotential = 0f;		    
		    
	        if (!branchHasSpikes) {
		        synapseBranch.active = false;
		        continue;
		    }
				
			boolean branchStayActive = false;
			for (Synapse synapse : synapseBranch.synapses) {
			    if (synapse.isEmpty()) continue;
			    stayActive = true;
			    branchStayActive = true;
			    synapse.accumulateSpikes(now, synapseBranch, this);
			    synapse.update(deltaTimeNanos);
			}
			synapseBranch.active = branchStayActive;
			
			// Update branch activity (low-pass filtered)
			synapseBranch.update(now, config);
		}

		// 2. Dendritic Competition (competition inside branch groups)
		if ( now-lastDendriticCompetitionProcessTime > 10_000_000l ) {
			Map<BranchType, List<SynapseBranch>> groups = groupBranchesByType();
	
			for (var entry : groups.entrySet()) {
				List<SynapseBranch> group = entry.getValue();
	
				if (group.size() < 2) continue;
	
				IDendriticCompetitionStrategy strategy = config.DENDIRITIC_COMPETITION_STRATEGIES.get(entry.getKey());
				if (strategy != null) {
					strategy.updateCompetition(
						group.toArray(new SynapseBranch[0]),
						deltaTimeNanos
					);
				}
			}
			lastDendriticCompetitionProcessTime = now;
		}

		// 3. Compute soma potential from all branches
		// Must be processed at every iteration!
		for (SynapseBranch synapseBranch : getInSynapseBranches()) {
			double windows = (double) deltaTimeNanos / config.INHIBITION_TAU_NANOS;
			synapseBranch.inhibition *= Maths.pow(config.INHIBITION_DECAY_PER_WINDOW, windows);
			
			if (synapseBranch.inhibition < 1e-9f)
			    synapseBranch.inhibition = 0f;
			
			float modulated = (synapseBranch.branchPotential * synapseBranch.gain)
				- synapseBranch.inhibition;

			somaPotential += modulated;
		}

		// 4. Firing decision (ONLY here)
		boolean inRefractory = (now - lastSpikeTime) < config.REFRACTORY_PERIOD_NANOS;
		if (!inRefractory && somaPotential > config.FIRING_THRESHOLD) {
			fire(now);
		}

		lastProcessTime = now;
		return stayActive;
	}

	// continuous/exponential decay based on elapsed time 
	public float getRecentFiringRate(long now) {
		NeuronSoA neuronStateSoA = getHemisphereCtx().neuronSoA;
		
		int index = getIndex();
		long dt = now - neuronStateSoA.lastRateUpdate[index];
		if (dt <= 0) return neuronStateSoA.firingRate[index];

		// Temporal normalization
		double windows = (double) dt / config.RATE_WINDOW_NANOS;
		neuronStateSoA.firingRate[index] *= Maths.pow(config.RATE_DECAY_PER_WINDOW, windows);

		// Avoid negative or too small values
		if (neuronStateSoA.firingRate[index] < 0 || neuronStateSoA.firingRate[index] < 1e-6f) {
			neuronStateSoA.firingRate[index] = 0;
		}
		neuronStateSoA.lastRateUpdate[index] = now;
		return neuronStateSoA.firingRate[index];
	}
}
