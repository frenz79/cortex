package com.cortex.base.soa.logic;

import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.SpikeRingBufferSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.brain.HemisphereContext;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuronLogic {

    protected final NeuronSoA neuronSoA;
    protected final SynapseBranchSoA synBranchSoA;
    protected final SynapseSoA synapseSoA;
    protected final SynapseTopologySoA synTopologySoA;
    protected final SynapseLogic synapseLogic;
    protected final SpikeRingBufferSoA spikeBuffer;
    protected final CombinedLateralInhibitionLogic combinedLateralInhibitionLogic;
    
    public AbstractNeuronLogic(int hemisphereId) {
        HemisphereContext ctx = HemisphereContext.get(hemisphereId);
        this.neuronSoA = ctx.neuronSoA;
        this.synBranchSoA = ctx.synapseBranchSoA;
        this.synapseSoA = ctx.synapseSoA;
        this.synTopologySoA = ctx.synapseTopologySoA;
        this.synapseLogic = ctx.synapseLogic;
        this.spikeBuffer = ctx.spikeBuffer;
        this.combinedLateralInhibitionLogic = ctx.combinedLateralInhibitionLogic;
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
                spikeBuffer.addSpike(
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
