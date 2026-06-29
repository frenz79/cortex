package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuronLogic {

    protected final NeuronSoA neuronSoA;
    protected final SynapseBranchSoA synBranchSoA;
    protected final SynapseTopologySoA synTopologySoA;
    protected final SynapseLogic synapseLogic;
    protected final SpikeRingBufferLogic spikeBufferLogic;
    protected final CombinedLateralInhibitionLogic combinedLateralInhibitionLogic;
    
    public AbstractNeuronLogic(
    	// Custom params
    		
    	// SoA dep
    	NeuronSoA neuronSoA,
    	SynapseBranchSoA synBranchSoA,
    	SynapseTopologySoA synTopologySoA,
    	// Logic dep
    	SynapseLogic synapseLogic,
    	SpikeRingBufferLogic spikeBufferLogic,
    	CombinedLateralInhibitionLogic combinedLateralInhibitionLogic    		
    ) {
        Objects.nonNull(neuronSoA);
        Objects.nonNull(synBranchSoA);
        Objects.nonNull(synTopologySoA);
        Objects.nonNull(synapseLogic);
        Objects.nonNull(spikeBufferLogic);
        Objects.nonNull(combinedLateralInhibitionLogic);
        
        this.neuronSoA = neuronSoA;
        this.synBranchSoA = synBranchSoA;
        this.synTopologySoA = synTopologySoA;
        this.synapseLogic = synapseLogic;
        this.spikeBufferLogic = spikeBufferLogic;
        this.combinedLateralInhibitionLogic = combinedLateralInhibitionLogic;
    }

    public abstract boolean process(long now, int index);

	// continuous/exponential decay based on elapsed time 
	public abstract float getRecentFiringRate(long now, int index);
	
    public void fire( long now, int index ) {
    	neuronSoA.setPendingFire(index);
    	combinedLateralInhibitionLogic.applyLateralInhibition(now, index);
    };
   
    // --- DELAYED FIRE ---
    public void delayedFire(long now, int neuronIndex, int[] incomingBranches, int[] outgoingBranches) {
        // 1. POST-SPIKE PLASTICITY
        for (int b : incomingBranches) {
            int start = synBranchSoA.synapseStart[b];
            int count = synBranchSoA.synapseCount[b];

            for (int i = start; i < start + count; i++) {
                int synId = synTopologySoA.synapseIndex[i];

                if (synapseLogic.wasFrequentlyActiveInLastWindow(synId)) {
                    synapseLogic.onPostSpike(synId, now, now );
                }
            }
        }

        // 2. OUTGOING SPIKE PROPAGATION
        for (int b : outgoingBranches) {
            int start = synBranchSoA.synapseStart[b];
            int count = synBranchSoA.synapseCount[b];

            for (int i = start; i < start + count; i++) {
                int synId = synTopologySoA.synapseIndex[i];
                long arrival = now + synapseLogic.getTraversalTimeNanos(now, synId);
                float amplitude = 1.0f; //neuronSoA.spikeAmplitude[neuronIndex];
                spikeBufferLogic.addSpike(
                    arrival,
                    synId,
                    amplitude,
                    neuronSoA.isInhibitory(neuronIndex),
                    now
                );
            }
        }

        // 3. UPDATE FIRING RATE
        neuronSoA.clearPendingFire(neuronIndex);
        neuronSoA.firingRate[neuronIndex] += 1.0f;
        neuronSoA.lastRateUpdate[neuronIndex] = now;

        EventBus.fire(EventType.NEURON_FIRED, now, neuronIndex, null);
    }
}
