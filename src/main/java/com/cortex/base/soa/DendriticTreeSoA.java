package com.cortex.base.soa;

public final class DendriticTreeSoA {

    // For each neuron: index of the first branch in the global branch arrays
    public int[] firstBranchIndex;

    // For each neuron: how many branches it has
    public byte[] branchCount;

    public DendriticTreeSoA(int totalNeurons) {
        firstBranchIndex = new int[totalNeurons];
        branchCount      = new byte[totalNeurons];
    }
}
