package com.cortex.base.soa;

public final class SynapseTopologySoA {

    // For each branch: where its synapses start in the global synapse arrays
    public int[] synapseStart;

    // For each branch: how many synapses it has
    public int[] synapseCount;

    // Optional: contiguous index list
    public int[] synapseIndex;

    public SynapseTopologySoA(int totalBranches, int totalSynapses) {
        synapseStart = new int[totalBranches];
        synapseCount = new int[totalBranches];
        synapseIndex = new int[totalSynapses];
    }
}
