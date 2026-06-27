package com.cortex.base.soa;

/**
 * Multiple producer - single consumer
 */
public final class SpikeRingBufferSoA {

    // Config
    public final int ringSize;
    public final long tickDurationNanos;

    // Per ogni bucket: un buffer di spike
    public final SpikeBufferSoA[] buckets;

    // Indice del bucket corrente (head)
    public int headIndex;

    public SpikeRingBufferSoA(int ringSize, int maxSpikesPerBucket, long tickDurationNanos) {
        this.ringSize = ringSize;
        this.tickDurationNanos = tickDurationNanos;
        this.buckets = new SpikeBufferSoA[ringSize];
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

        // Thread-safe append
        buckets[bucketIndex].addSpike(arrival, synId, amp, inhibitory);
    }
    
    public int pollAndProcess(long now, SynapseSoA syn, SynapseBranchSoA branch) {
        SpikeBufferSoA bucket = buckets[headIndex];
        int processed = 0;

        for (int i = 0; i < bucket.size; i++) {
            long arrival = bucket.arrivalTimeNanos[i];
            if (arrival > now) {
                // opzionale: in un ring “ideale” non dovrebbe succedere
                continue;
            }

            int synId = bucket.synapseId[i];
            int bId   = syn.postBranchId[synId];

            branch.branchPotential[bId] += bucket.amplitude[i] * syn.weight[synId];
            processed++;
        }

        // reset del bucket dopo il consumo
        bucket.size = 0;

        // avanzamento della testa
        headIndex = (headIndex + 1) % ringSize;

        return processed;
    }
}
