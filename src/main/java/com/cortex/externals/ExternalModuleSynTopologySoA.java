package com.cortex.externals;

public class ExternalModuleSynTopologySoA {
    public final int[] synapseStart;   // 
    public final int[] synapseCount;   // 
    public final int[] synapseIndex;   // 

    public ExternalModuleSynTopologySoA(int totalUnits, int totalSynapses) {
        this.synapseStart = new int[totalUnits];
        this.synapseCount = new int[totalUnits];
        this.synapseIndex = new int[totalSynapses];
    }
}
