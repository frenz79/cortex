package com.cortex.base.soa;

// Hot fields SOA
public final class SynapseStateSoA {
    public long[] lastDecayTime;
    public float[] myelinFactor;
    public int[] activityCounter;
    public int[] writeIndex;
    public int[] readIndex;
    public int[] branchIndex;
    
    public SynapseStateSoA(int totalSynapses) {
    	if (lastDecayTime!=null || myelinFactor!=null || activityCounter!=null || writeIndex!=null || readIndex!=null)
    		throw new RuntimeException("Buffer has already been allocated");
    	allocate(totalSynapses);
    }
    
    public SynapseStateSoA() {
    	// No allocation
    }
    
    public void allocate(int totalSynapses) {
        lastDecayTime = new long[totalSynapses];
        myelinFactor = new float[totalSynapses];
        activityCounter = new int[totalSynapses];
        writeIndex = new int[totalSynapses];
        readIndex = new int[totalSynapses];
        branchIndex = new int[totalSynapses];
    }
}