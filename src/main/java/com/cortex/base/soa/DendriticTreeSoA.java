package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public final class DendriticTreeSoA {

    // For each neuron: index of the first branch in the global branch arrays
	@SerializableAttribute
	public final int[] firstBranchIndex;

    // For each neuron: how many branches it has
	@SerializableAttribute
    public final byte[] branchCount;

    public DendriticTreeSoA(int totalNeurons) {
        firstBranchIndex = new int[totalNeurons];
        branchCount      = new byte[totalNeurons];
    }
}
