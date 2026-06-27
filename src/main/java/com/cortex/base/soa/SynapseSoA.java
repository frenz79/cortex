package com.cortex.base.soa;

public final class SynapseSoA {

    // --- Topology ---
    public int[] preNeuronId;      // source neuron
    public int[] postNeuronId;     // target neuron
    public int[] postBranchId;     // target branch (dendritic)
    public float length[];
    public float baseSpeed[];
    
    // --- Dynamics ---
    public float[] weight;         // synaptic weight
    public float[] eligibility;    // eligibility trace
    public long[] lastPreSpikeTime;
    public long[] lastPostSpikeTime;

    // --- Delay / conduction ---
    public int[] delayNanos;       // conduction delay
    public float[] myelinFactor;   // conduction speed modifier

    // --- Activity ---
    public int[] activityCounter;
    public long[] lastDecayTime;

    // --- Spike buffer indices (if using per-synapse ring buffer) ---
    public int[] writeIndex;
    public int[] readIndex;

    // --- Routing ---
    public int[] branchIndex;      // redundant with postBranchId, but useful for fast lookup

    // --- Flags ---
    public byte[] flags;

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
    }
}
