package com.cortex.base.soa.logic;

import java.util.Arrays;
import java.util.Objects;

import com.cortex.base.soa.SpikeBufferSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.utils.Maths;

public class SpikeRingBufferLogic {

	private final SynapseSoA synapseSoA;
	private final SynapseBranchSoA synapseBranchSoA;
	
    // Config params
    public final int ringSize;
    public final long tickDurationNanos;

    // A SpikeBufferSoA for each bucket
    public final SpikeBufferSoA[] buckets;

    public int[] synapseSpikeCount;
    public float[] synapseLastAmplitude;
    
    // Head
    public int headIndex;

    public SpikeRingBufferLogic(
    	int ringSize, 
    	int maxSpikesPerBucket, 
    	long tickDurationNanos,
    	
    	SynapseSoA synapseSoA,
    	SynapseBranchSoA synapseBranchSoA
    ) throws Exception {
    	Objects.nonNull(synapseSoA);
    	Objects.nonNull(synapseBranchSoA);
    	
        this.ringSize = ringSize;
        if (!Maths.isPowerOfTwo(ringSize))
        	throw new RuntimeException("ringSize must be a power of 2");
        
        this.tickDurationNanos = tickDurationNanos;
        this.buckets = new SpikeBufferSoA[ringSize];

        this.synapseSoA = synapseSoA;
        this.synapseBranchSoA = synapseBranchSoA;
        
        int totalSynapses = synapseSoA.totalSynapses;
        if (totalSynapses==0)
        	throw new RuntimeException("SynapseSoA not initialized");
	
        this.synapseSpikeCount = new int[totalSynapses];
        this.synapseLastAmplitude = new float[totalSynapses];

        for (int i = 0; i < ringSize; i++) {
            buckets[i] = new SpikeBufferSoA(maxSpikesPerBucket);
        }

        this.headIndex = 0;
    }
    
    public void addSpike(long arrival, int synId, float amp, boolean inhibitory, long now) {
        long delta = arrival - now;
        if (delta < 0) delta = 0;

        long ticksAhead = delta / tickDurationNanos;
        int bucketOffset = (int) (ticksAhead % ringSize);
        int bucketIndex = (headIndex + bucketOffset) % ringSize;

        synapseSpikeCount[synId]++;
        synapseLastAmplitude[synId] = amp;
        
        // Thread-safe append
        buckets[bucketIndex].addSpike(arrival, synId, amp, inhibitory);
    }
    
    public int pollAndProcess(long now) {
        SpikeBufferSoA bucket = buckets[headIndex];
        int processed = 0;

        for (int i = 0; i < bucket.size; i++) {
            long arrival = bucket.arrivalTimeNanos[i];
            if (arrival > now) continue;

            int synId = bucket.synapseId[i];
            int bId   = synapseSoA.targetBranchId[synId];

            synapseBranchSoA.branchPotential[bId] += bucket.getAmplitude(i) * synapseSoA.weight[synId];
            processed++;
        }

        // reset bucket
        bucket.size = 0;

        // reset data
        Arrays.fill(synapseSpikeCount, 0);
        Arrays.fill(synapseLastAmplitude, 0f);

        // move head fwd
        headIndex = (headIndex + 1) % ringSize;
        return processed;
    }


	public boolean hasSpikeForSynapse(int synId) {
		return synapseSpikeCount[synId]>0;
	}

	public float getLastSpikeAmplitudeForSynapse(int synId) {
		return synapseLastAmplitude[synId];
	}
}
