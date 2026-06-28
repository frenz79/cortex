package com.cortex.base.soa;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Multidimensional Spike SoA
 * 
 */
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

    // intensityMask bitmap
    // [31..16] amplitude (16 bit)
	// [15..8]  counter   (8 bit)
	// [7..6]   frequency (2 bit)
	// [5]      saliency
	// [4]      error
	// [3..2]   type (2 bit)
	// [1]      inhibitory
	// [0]      burst
    public final int[] intensityMask;

    public static final int NO_FLAG   = 0; // 00
    
    // TYPE (2 bit)
    public static final int TYPE_NONE   = 0 << 2; // 00
    public static final int TYPE_A      = 1 << 2; // 01
    public static final int TYPE_B      = 2 << 2; // 10
    public static final int TYPE_C      = 3 << 2; // 11

    // FLAGS (bit bassi)
    public static final int FLAG_INHIBITORY = 1 << 1; // bit 1
    public static final int FLAG_BURST      = 1 << 0; // bit 0

    // SEMANTIC (in frequency nibble)
    public static final int FLAG_SALIENCY   = 1 << 5; // bit 5
    public static final int FLAG_ERROR      = 1 << 4; // bit 4
    
    private final VarHandle sizeHandle;
    
    public SpikeBufferSoA(int maxSpikes) throws NoSuchFieldException, IllegalAccessException {
    	this.sizeHandle = MethodHandles.lookup().findVarHandle(SpikeBufferSoA.class, "size", int.class);
        arrivalTimeNanos = new long[maxSpikes];
        synapseId        = new int[maxSpikes];
        intensityMask    = new int[maxSpikes];
        size             = 0;
    }

    private int addSpikeIndex() {
    	return (int) sizeHandle.getAndAdd(this, 1);
    }
    
    public static int buildMask(
            int amplitude16,
            int counter8,
            int frequency2,
            boolean saliency,
            boolean error,
            int type2bit,
            boolean inhibitory,
            boolean burst
    ) {
        int mask = 0;

        mask |= (amplitude16 & 0xFFFF) << 16;
        mask |= (counter8    & 0xFF)   << 8;

        // frequency (2 bit) → bits [7..6]
        mask |= (frequency2 & 0x3) << 6;

        // saliency → bit 5
        if (saliency) mask |= FLAG_SALIENCY;

        // error → bit 4
        if (error) mask |= FLAG_ERROR;

        // type (2 bit) → bits [3..2]
        mask |= (type2bit & 0x3) << 2;

        // inhibitory → bit 1
        if (inhibitory) mask |= FLAG_INHIBITORY;

        // burst → bit 0
        if (burst) mask |= FLAG_BURST;

        return mask;
    }
    
    public static short amplitudeToShort(float amp) {
        float a = amp;
        a = a < 0f ? 0f : a;
        a = a > 1f ? 1f : a;
        return (short) (a * 65535f);
    }
    
    public int addSpike(long arrival, int synId, float amplitude16, boolean inhibitory) {
    	return addSpike(arrival, synId, buildMask(amplitudeToShort(amplitude16),1,0,false,false,NO_FLAG, inhibitory, false) );
    }
    
    public int addSpike(long arrival, int synId, int amplitude16, boolean inhibitory) {
    	return addSpike(arrival, synId, buildMask(amplitude16,1,0,false,false,NO_FLAG, inhibitory, false) );
    }
    
    public int addSpike(long arrival, int synId, int amplitude16, int counter8, int frequency2, boolean saliency,
            boolean error,
            int type2bit,
            boolean inhibitory,
            boolean burst) {
    	return addSpike(arrival, synId, buildMask(amplitude16,counter8,frequency2,saliency,error,type2bit,inhibitory,burst ) );
    }
    
    public int addSpike(long arrival, int synId, int mask) {
        int idx = addSpikeIndex();
        assert idx < arrivalTimeNanos.length;
        arrivalTimeNanos[idx] = arrival;
        synapseId[idx]        = synId;
        intensityMask[idx]    = mask;
        return idx;
    }
    
    // Decode components
    public static int decodeAmplitude(int mask) {
        return (mask >>> 16) & 0xFFFF;
    }

    public static int decodeCounter(int mask) {
        return (mask >>> 8) & 0xFF;
    }

    public static int decodeFrequency(int mask) {
        return (mask >>> 6) & 0x3; // 2 bit
    }

    public static boolean decodeSalient(int mask) {
        return (mask & FLAG_SALIENCY) != 0;
    }

    public static boolean decodeError(int mask) {
        return (mask & FLAG_ERROR) != 0;
    }

    public static int decodeType(int mask) {
        return (mask >>> 2) & 0x3;
    }

    public static boolean decodeInhibitory(int mask) {
        return (mask & FLAG_INHIBITORY) != 0;
    }

    public static boolean decodeBurstMarked(int mask) {
        return (mask & FLAG_BURST) != 0;
    }

    
    // Encode components
    public int setAmplitude(int idx, int amplitude16) {
        intensityMask[idx] = (intensityMask[idx] & 0x0000FFFF) | ((amplitude16 & 0xFFFF) << 16);
        return intensityMask[idx];
    }

    public int setCounter(int idx, int counter8) {
    	intensityMask[idx] = (intensityMask[idx] & 0xFFFF00FF) | ((counter8 & 0xFF) << 8);
    	return intensityMask[idx];
    }

    public int setFrequency(int idx, int frequency2) {
        intensityMask[idx] = (intensityMask[idx] & 0xFFFFFF3F) // clear bits 7..6
                           | ((frequency2 & 0x3) << 6);
        return intensityMask[idx];
    }
    
    public int setInhibitory(int idx, boolean inhibitory) {
        if (inhibitory)
            intensityMask[idx] |= FLAG_INHIBITORY;
        else
            intensityMask[idx] &= ~FLAG_INHIBITORY;
        return intensityMask[idx];
    }
    
    public int setBurst(int idx, boolean burst) {
        if (burst)
            intensityMask[idx] |= FLAG_BURST;
        else
            intensityMask[idx] &= ~FLAG_BURST;
        return intensityMask[idx];
    }
    
    public int setSaliency(int idx, boolean salient) {
        if (salient)
            intensityMask[idx] |= FLAG_SALIENCY;
        else
            intensityMask[idx] &= ~FLAG_SALIENCY;
        return intensityMask[idx];
    }
    
    public int setError(int idx, boolean error) {
        if (error)
            intensityMask[idx] |= FLAG_ERROR;
        else
            intensityMask[idx] &= ~FLAG_ERROR;
        return intensityMask[idx];
    }
        
    public int setType(int idx, int type2bit) {
        intensityMask[idx] = (intensityMask[idx] & 0xFFFFFFF3) // clear bits 3..2
                           | ((type2bit & 0x3) << 2);
        return intensityMask[idx];
    }
    
    public int setFlags(int idx, int flags4) {
        intensityMask[idx] = (intensityMask[idx] & 0xFFFFFFF0) | (flags4 & 0xF);
        return intensityMask[idx];
    }
    
    // 
    public float getAmplitude(int idx) {
        int amp16 = (intensityMask[idx] >>> 16) & 0xFFFF;
        return amp16 / 65535f;
    }

    public int getCounter(int idx) {
        return (intensityMask[idx] >>> 8) & 0xFF;
    }

    public int getFrequency(int idx) {
        return (intensityMask[idx] >>> 6) & 0x3; // 2 bit
    }

    public boolean isSalient(int idx) {
        return (intensityMask[idx] & FLAG_SALIENCY) != 0;
    }

    public boolean isError(int idx) {
        return (intensityMask[idx] & FLAG_ERROR) != 0;
    }

    public int getType(int idx) {
        return (intensityMask[idx] >>> 2) & 0x3;
    }

    public boolean isInhibitory(int idx) {
        return (intensityMask[idx] & FLAG_INHIBITORY) != 0;
    }

    public boolean isBurst(int idx) {
        return (intensityMask[idx] & FLAG_BURST) != 0;
    }
}
