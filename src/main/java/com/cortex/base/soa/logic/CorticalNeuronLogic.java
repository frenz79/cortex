package com.cortex.base.soa.logic;

import com.cortex.base.AbstractNeuron;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;

public final class CorticalNeuronLogic extends AbstractNeuronLogic {

    public CorticalNeuronLogic(int hemisphereId) {
        super(hemisphereId);
    }

    @Override
    public void process(AbstractNeuron neuron, long now) {
        // dendritic integration
        // inhibition decay
        // threshold check
        // bursting logic
        // adaptation
    }

    @Override
    public void fire(AbstractNeuron neuron, long now) {
        neuronState.setPendingFire(neuron.getIndex());

        // lateral inhibition
        // event bus
    }

    @Override
    public void delayedFire(AbstractNeuron neuron, long now) {
        int idx = neuron.getIndex();

        // 1. post-spike plasticity
        for (int b : neuron.getInSynapseBranchIndices()) {
            int start = branchState.synapseStart[b];
            int count = branchState.synapseCount[b];
            for (int i = start; i < start + count; i++) {
                int synId = synTopology.synapseIndex[i];
                if (synapseState.wasFrequentlyActiveInLastWindow[synId]) {
                    synapseLogic.onPostSpike(synId, now, synapseState.plasticity);
                }
            }
        }

        // 2. outgoing spike propagation
        for (int b : neuron.getOutSynapseBranchIndices()) {
            int start = branchState.synapseStart[b];
            int count = branchState.synapseCount[b];
            for (int i = start; i < start + count; i++) {
                int synId = synTopology.synapseIndex[i];
                long arrival = now + synapseLogic.getTraversalTimeNanos(now, synId);
                spikeRing.addSpike(arrival, synId, neuron.getSpikeSign(), (byte)0, now);
            }
        }

        // 3. update firing rate
        neuronState.clearPendingFire(idx);
        neuronState.firingRate[idx] += 1.0f;
        neuronState.lastRateUpdate[idx] = now;

        EventBus.fire(EventType.NEURON_FIRED, now, neuron, null);
    }
}
