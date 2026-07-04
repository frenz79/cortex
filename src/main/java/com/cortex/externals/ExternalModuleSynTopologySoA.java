package com.cortex.externals;

public class ExternalModuleSynTopologySoA {
    public final int[] synapseStart;   // 
    public final int[] synapseCount;   // 
    public final int[] synapseIndex;   // 

    private final int totalSynapses;
    
    public ExternalModuleSynTopologySoA(int totalUnits, int totalSynapses) {
        this.synapseStart = new int[totalUnits];
        this.synapseCount = new int[totalUnits];
        this.synapseIndex = new int[totalSynapses];
        this.totalSynapses = totalSynapses;
    }

	public int getTotalSynapses() {
		return totalSynapses;
	}
}
