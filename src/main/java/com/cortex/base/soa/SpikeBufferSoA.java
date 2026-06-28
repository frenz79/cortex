package com.cortex.base.soa;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class SpikeBufferSoA {

    // Padding to avoid false sharing
    long p0, p1, p2, p3, p4, p5, p6;
    public volatile int size;
    // Padding to isolate size in its own cache line
    long p7, p8, p9, p10, p11, p12, p13;
    
    // When the spike will arrive at the target synapse
    public final long[] arrivalTimeNanos;

    // Which synapse this spike belongs to
    public final int[] synapseId;

    // Spike amplitude (float, not double)
    public final float[] amplitude;

    // Bitmask
    private static final byte INHIBITORY_MASK  = 0b00000001;
    public final byte[] flags;

    private final VarHandle sizeHandle;
    
    public SpikeBufferSoA(int maxSpikes) throws NoSuchFieldException, IllegalAccessException {
    	this.sizeHandle = MethodHandles.lookup().findVarHandle(SpikeBufferSoA.class, "size", int.class);
        arrivalTimeNanos = new long[maxSpikes];
        synapseId        = new int[maxSpikes];
        amplitude        = new float[maxSpikes];
        flags            = new byte[maxSpikes];
        size             = 0;
    }

    private int addSpikeIndex() {
    	return (int) sizeHandle.getAndAdd(this, 1);
    }
    
    public void addSpike(long arrival, int synId, float amp, boolean inhibitory) {
        int idx = addSpikeIndex();
        arrivalTimeNanos[idx] = arrival;
        synapseId[idx]        = synId;
        amplitude[idx]        = amp;
        flags[idx] = inhibitory ? INHIBITORY_MASK : 0;
    }
    
    public void setInhibitory(int idx) {
        flags[idx] |= INHIBITORY_MASK;
    }

    public void clearInhibitory(int idx) {
        flags[idx] &= ~INHIBITORY_MASK;
    }

    public boolean isInhibitory(int idx) {
        return (flags[idx] & INHIBITORY_MASK) != 0;
    }
}
