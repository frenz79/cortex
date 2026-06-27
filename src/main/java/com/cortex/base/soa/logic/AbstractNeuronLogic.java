package com.cortex.base.soa.logic;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.SpikeRingBufferSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.brain.HemisphereContext;

public abstract class AbstractNeuronLogic {

    protected final NeuronSoA neuronState;
    protected final SynapseBranchSoA branchState;
    protected final SynapseSoA synapseState;
    protected final SynapseTopologySoA synTopology;
    protected final SynapseLogic synapseLogic;
    protected final SpikeRingBufferSoA spikeRing;

    public AbstractNeuronLogic(int hemisphereId) {
        HemisphereContext ctx = HemisphereContext.get(hemisphereId);
        this.neuronState = ctx.neuronSoA;
        this.branchState = ctx.synapseBranchSoA;
        this.synapseState = ctx.synapseSoA;
        this.synTopology = ctx.synapseTopologySoA;
        this.synapseLogic = ctx.synapseLogic;
        this.spikeRing = ctx.spikeRingBuffer;
    }

    public abstract void process(AbstractNeuron neuron, long now);

    public abstract void fire(AbstractNeuron neuron, long now);

    public abstract void delayedFire(AbstractNeuron neuron, long now);
}
