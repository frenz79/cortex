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
    public long[] arrivalTimeNanos;

    // Which synapse this spike belongs to
    public int[] synapseId;

    // Spike amplitude (float, not double)
    public float[] amplitude;

    // Optional flags (plasticity, type, etc.)
    public byte[] flags;

    private static VarHandle sizeHandle;
    static {
    	try {
			sizeHandle = MethodHandles.lookup().findVarHandle(SpikeBufferSoA.class, "size", int.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }    
    
    public SpikeBufferSoA(int maxSpikes) {
        arrivalTimeNanos = new long[maxSpikes];
        synapseId        = new int[maxSpikes];
        amplitude        = new float[maxSpikes];
        flags            = new byte[maxSpikes];
        size             = 0;
    }

    public int addSpikeIndex() {
    	return (int) sizeHandle.getAndAdd(this, 1);
    }
    
    public void addSpike(long arrival, int synId, float amp, byte f) {
        int idx = addSpikeIndex();
        arrivalTimeNanos[idx] = arrival;
        synapseId[idx]        = synId;
        amplitude[idx]        = amp;
        flags[idx]            = f;
    }
}
