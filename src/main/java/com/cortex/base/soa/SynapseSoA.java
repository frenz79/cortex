package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public final class SynapseSoA {

    // --- Topology ---
	@SerializableAttribute
    public final int[] preNeuronId;      // source neuron
	@SerializableAttribute
    public final int[] postNeuronId;     // target neuron
	@SerializableAttribute
    public final int[] postBranchId;     // target branch (dendritic)
	@SerializableAttribute
    public final float length[];
	@SerializableAttribute
    public final float baseSpeed[];
    
    // --- Dynamics ---
	@SerializableAttribute
    public final float[] weight;         // synaptic weight
	@SerializableAttribute
    public final float[] eligibility;    // eligibility trace
	@SerializableAttribute
    public final long[] lastPreSpikeTime;
	@SerializableAttribute
    public final long[] lastPostSpikeTime;

    // --- Delay / conduction ---
	@SerializableAttribute
    public final int[] delayNanos;       // conduction delay
	@SerializableAttribute
    public final float[] myelinFactor;   // conduction speed modifier

    // --- Activity ---
	@SerializableAttribute
    public final int[] activityCounter;
	@SerializableAttribute
    public final long[] lastDecayTime;

    // --- Spike buffer indices (if using per-synapse ring buffer) ---
	@SerializableAttribute
    public final int[] writeIndex;
	@SerializableAttribute
    public final int[] readIndex;

    // --- Routing ---
	@SerializableAttribute
    public final int[] branchIndex;      // redundant with postBranchId, but useful for fast lookup

    // --- Flags ---
	@SerializableAttribute
    public final byte[] flags;

	@SerializableAttribute
    public final int totalSynapses;
    
    public SynapseSoA(int totalSynapses) {
        preNeuronId      = new int[totalSynapses];
        postNeuronId     = new int[totalSynapses];
        postBranchId     = new int[totalSynapses];
        length           = new float[totalSynapses];
        baseSpeed        = new float[totalSynapses];
        
        weight           = new float[totalSynapses];
        eligibility      = new float[totalSynapses];
        lastPreSpikeTime = new long[totalSynapses];
        lastPostSpikeTime= new long[totalSynapses];

        delayNanos       = new int[totalSynapses];
        myelinFactor     = new float[totalSynapses];

        activityCounter  = new int[totalSynapses];
        lastDecayTime    = new long[totalSynapses];

        writeIndex       = new int[totalSynapses];
        readIndex        = new int[totalSynapses];

        branchIndex      = new int[totalSynapses];
        flags            = new byte[totalSynapses];
        
        this.totalSynapses = totalSynapses;
    }
}
