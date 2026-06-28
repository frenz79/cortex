package com.cortex.base.soa;

public final class SynapseSoA {

    // --- Topology ---
    public final int[] preNeuronId;      // source neuron
    public final int[] postNeuronId;     // target neuron
    public final int[] postBranchId;     // target branch (dendritic)
    public final float length[];
    public final float baseSpeed[];
    
    // --- Dynamics ---
    public final float[] weight;         // synaptic weight
    public final float[] eligibility;    // eligibility trace
    public final long[] lastPreSpikeTime;
    public final long[] lastPostSpikeTime;

    // --- Delay / conduction ---
    public final int[] delayNanos;       // conduction delay
    public final float[] myelinFactor;   // conduction speed modifier

    // --- Activity ---
    public final int[] activityCounter;
    public final long[] lastDecayTime;

    // --- Spike buffer indices (if using per-synapse ring buffer) ---
    public final int[] writeIndex;
    public final int[] readIndex;

    // --- Routing ---
    public final int[] branchIndex;      // redundant with postBranchId, but useful for fast lookup

    // --- Flags ---
    public final byte[] flags;

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
