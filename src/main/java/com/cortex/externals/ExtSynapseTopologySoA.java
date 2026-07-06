package com.cortex.externals;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public final class ExtSynapseTopologySoA {

    // For each neuron: where its synapses start in the global synapse arrays
	@SerializableAttribute
	public final int[] synapseStart;

    // For each neuron: how many synapses it has
	@SerializableAttribute
    public final byte[] synapseCount;

    public ExtSynapseTopologySoA( int totalNeurons) {
        synapseStart = new int[totalNeurons];
        synapseCount = new byte[totalNeurons];
    }
}
