package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.NeuronTopologySoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public abstract class AbstractNeuronLogic {

    protected final NeuronSoA neuronSoA;
    protected final NeuronTopologySoA neuronTopologySoA;
    protected final SynapseBranchSoA synBranchSoA;
    protected final SynapseTopologySoA synTopologySoA;
    protected final SynapseLogic synapseLogic;
    protected final SpikeRingBufferLogic spikeBufferLogic;
    protected final CombinedLateralInhibitionLogic combinedLateralInhibitionLogic;
    
    public AbstractNeuronLogic(
    	// Custom params
    		
    	// SoA dep
    	NeuronSoA neuronSoA,
    	NeuronTopologySoA neuronTopologySoA,
    	SynapseBranchSoA synBranchSoA,
    	SynapseTopologySoA synTopologySoA,
    	// Logic dep
    	SynapseLogic synapseLogic,
    	SpikeRingBufferLogic spikeBufferLogic,
    	CombinedLateralInhibitionLogic combinedLateralInhibitionLogic    		
    ) {
        Objects.nonNull(neuronSoA);
        Objects.nonNull(neuronTopologySoA);
        Objects.nonNull(synBranchSoA);
        Objects.nonNull(synTopologySoA);
        Objects.nonNull(synapseLogic);
        Objects.nonNull(spikeBufferLogic);
        Objects.nonNull(combinedLateralInhibitionLogic);
        
        this.neuronSoA = neuronSoA;
        this.neuronTopologySoA = neuronTopologySoA;
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
    	combinedLateralInhibitionLogic.process(now, index);
    };
   
    public void delayedFire(long now, int neuronIndex) {
        // 1. POST-SPIKE PLASTICITY (incoming branches)
        int inStart = neuronTopologySoA.incomingBranchStart[neuronIndex];
        int inCount = neuronTopologySoA.incomingBranchCount[neuronIndex];

        for (int bi = inStart; bi < inStart + inCount; bi++) {
            int branch = neuronTopologySoA.incomingBranches[bi];

            int synStart = synBranchSoA.synapseStart[branch];
            int synCount = synBranchSoA.synapseCount[branch];

            for (int si = synStart; si < synStart + synCount; si++) {
                int synId = synTopologySoA.synapseIndex[si];

                if (synapseLogic.wasFrequentlyActiveInLastWindow(synId)) {
                    synapseLogic.onPostSpike(synId, now, now);
                }
            }
        }

        // 2. OUTGOING SPIKE PROPAGATION
        int outStart = neuronTopologySoA.outgoingBranchStart[neuronIndex];
        int outCount = neuronTopologySoA.outgoingBranchCount[neuronIndex];

        for (int bi = outStart; bi < outStart + outCount; bi++) {
            int branch = neuronTopologySoA.outgoingBranches[bi];

            int synStart = synBranchSoA.synapseStart[branch];
            int synCount = synBranchSoA.synapseCount[branch];

            for (int si = synStart; si < synStart + synCount; si++) {
                int synId = synTopologySoA.synapseIndex[si];

                long arrival = now + synapseLogic.getTraversalTimeNanos(now, synId);
                float amplitude = 1.0f; // oppure neuronSoA.spikeAmplitude[neuronIndex];

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
