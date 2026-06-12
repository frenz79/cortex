package com.cortex.brain;

import java.util.List;
import java.util.Map;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.cortex.base.SynapseBranch;
import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.lateral_inhibition.ILateralInhibitionStrategy;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

/**
 *  Event-driven, analog-spike, delayed, plastic Neuron
 * 
 * */
public class CorticalNeuron extends AbstractNeuron {

	private long lastProcessTime = System.nanoTime();
	private long lastSpikeTime = 0l;
	private float potential = 0;

	private final CorticalNeuronsConfig config;

	public CorticalNeuron(int index, int layerId, boolean hasIncoming, boolean hasOutgoing, CorticalNeuronsConfig neuronsConfig, boolean inhibitor, Point3f position) {
		super(layerId, 
				index, 
				hasIncoming, 
				hasOutgoing, 
				inhibitor, 
				position
				);
		this.config = neuronsConfig;
	}

	// Decadimento lineare verso il potenziale di riposo
	private void computeDecay( double deltaTimeNanos ) {
		if (potential == config.POTENTIAL_ZERO) return;
		double delta = config.REPOLARIZATION_PER_NANOS * deltaTimeNanos;
		if (potential > config.POTENTIAL_ZERO) {
			potential -= delta;
			if (potential < config.POTENTIAL_ZERO) potential = config.POTENTIAL_ZERO;
		} else {
			potential += delta;
			if (potential > config.POTENTIAL_ZERO) potential = config.POTENTIAL_ZERO;
		}
	}

	/**
	 */
	private boolean integrateInputAndFire(long now, Spike spike, Synapse synapse) {
		synapse.onPreSpike(now);
		float spikeIntensity = synapse.getWeight() * spike.amplitude();
		potential += spike.getSign() * spikeIntensity;
		if ( potential > config.FIRING_THRESHOLD) {
			lastSpikeTime = now;
			potential = config.POTENTIAL_ZERO;
			return true;
		}
		return false;
	}

	/**
	 * TODO:
	 * - doppio modello di potenziale potential vs somaPotential 
	 * - remove group.toArray(new SynapseBranch[0])
	 * - competition dentro il loop
FASE1 
for (branch : branches) {
    compute branchPotential
    update branchActivity
    update gain
}

FASE2
Map<BranchType, List<SynapseBranch>> groups = groupBranchesByType();

for (group : groups) {
    strategy.updateInhibition(group, dt);
}

FASE 3 — somma nei soma
for (branch : branches) {
    soma += (branchPotential - inhibition) * gain;
}


	 *
	 *
	 *
	 */	
	@Override
	public boolean process(long now) throws InterruptedException {
		long deltaTimeNanos = now - lastProcessTime;
		computeDecay(deltaTimeNanos);

		boolean[] fired = new boolean[] {false};
		boolean stayActive = false;
		boolean inRefractory = (now - lastSpikeTime) < config.REFRACTORY_PERIOD_NANOS;
		float somaPotential = 0f;
		
		// 1. Process ONLY synapses that have spikes
		for (SynapseBranch synapseBranch : getInSynapseBranches()) {
	        // Branch potential reset
			synapseBranch.branchPotential = 0f;
	        
			for ( Synapse synapse : synapseBranch.synapses ) {
				// Fast check: skip empty synapses
				if (synapse.isEmpty()) continue;

				stayActive = true;
				synapse.forEachSpike(now, spike -> {
					synapseBranch.branchPotential += spike.signedAmplitude();
					
					boolean fire = integrateInputAndFire(now, spike, synapse);
					if (fire && !inRefractory) {
						fired[0] = true;						
					}
				});
				// Update ONLY synapses that had spikes or are active
				synapse.update(deltaTimeNanos);
			}
			
	        // Update branch activity with decay
			synapseBranch.branchActivity = synapseBranch.branchActivity * 0.95f + synapseBranch.branchPotential;
			// dt / tau
			float alpha = Maths.clamp((float)deltaTimeNanos / (float)config.BRANCH_GAIN_TAU_NANOS, 0.0f, 1.0f);
            float error = synapseBranch.branchActivity - config.BRANCH_TARGET_ACTIVITY;
	        // update gain
	        synapseBranch.gain += alpha * error;
	        // clamp
	        synapseBranch.gain = Maths.clamp(synapseBranch.gain, config.BRANCH_GAIN_MIN, config.BRANCH_GAIN_MAX);
	        
	        Map<BranchType, List<SynapseBranch>> groups = groupBranchesByType();

	        for (var entry : groups.entrySet()) {

	            List<SynapseBranch> group = entry.getValue();

	            // no competition if only 1 branch
	            if (group.size() < 2) continue;

	            ILateralInhibitionStrategy strategy = inhibitionStrategies.get(entry.getKey());
	            if (strategy != null) {
	                strategy.updateInhibition(
	                    group.toArray(new SynapseBranch[0]),
	                    deltaTimeNanos
	                );
	            }
	        }

	        // Apply branch gain and inhibition
			float modulated = (synapseBranch.branchPotential * synapseBranch.gain) - synapseBranch.inhibition;
	        // Accumulate into soma potential
	        somaPotential += modulated;
		}
	    
		// 2. If neuron fires, notify ONLY synapses that had pre/post pairing
		if (fired[0] || somaPotential > config.FIRING_THRESHOLD) {
			fire(now); // just pendingFire = true
		}

		potential = Maths.clamp(potential,	config.POTENTIAL_MIN,config.POTENTIAL_MAX);
		lastProcessTime = now;
		return stayActive;
	}

	// continuous/exponential decay based on elapsed time 
	public float getRecentFiringRate(long now) {
		long dt = now - state.lastRateUpdate;
		if (dt <= 0) return state.firingRate;

		// Temporal normalization
		double windows = (double) dt / config.RATE_WINDOW_NANOS;
		state.firingRate *= Maths.pow(config.RATE_DECAY_PER_WINDOW, windows);

		// Avoid negative or too small values
		if (state.firingRate < 0 || state.firingRate < 1e-6f) {
			state.firingRate = 0;
		}
		state.lastRateUpdate = now;
		return state.firingRate;
	}
}
