package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public final class SynapseTopologySoA {

    // For each branch: where its synapses start in the global synapse arrays
	@SerializableAttribute
	public final int[] synapseStart;

    // For each branch: how many synapses it has
	@SerializableAttribute
    public final byte[] synapseCount;

    // Optional: contiguous index list
	@SerializableAttribute
    public final int[] synapseIndex;

    public SynapseTopologySoA(int totalBranches, int totalSynapses) {
        synapseStart = new int[totalBranches];
        synapseCount = new byte[totalBranches];
        synapseIndex = new int[totalSynapses];
    }
}
