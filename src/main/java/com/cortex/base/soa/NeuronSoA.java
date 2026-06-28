package com.cortex.base.soa;

// Hot fields grouped together for better locality
public final class NeuronSoA {

    // 3D position
    public final float[] posX;
    public final float[] posY;
    public final float[] posZ;
    
	public final float[] firingRate;
	public final long[]  lastRateUpdate;
	public final long[]  lastSpikeTime;
	public final long[]  lastProcessTime;
	
	// bits 0..4  → layerId (0–31)
	// bit  5     → isActive
	// bit  6     → pendingFire
	// bit  7     → inhibitory
	public final byte[] flags;
	
	public NeuronSoA(int totalNeurons) {
		this.posX = new float[totalNeurons];
		this.posY = new float[totalNeurons];
		this.posZ = new float[totalNeurons];
		this.firingRate = new float[totalNeurons];
		this.lastRateUpdate = new long[totalNeurons];
		this.lastSpikeTime = new long[totalNeurons];
		this.lastProcessTime = new long[totalNeurons];
		this.flags = new byte[totalNeurons];
	}

	public void setLayerId(int neuronId, int layer) {
        flags[neuronId] = (byte)((flags[neuronId] & 0b11100000) | (layer & 0b00011111));
    }

    public int getLayerId(int neuronId) {
        return flags[neuronId] & 0b00011111;
    }
    
    public void setActive(int neuronId) {
        flags[neuronId] |= 0b00100000;
    }

    public void clearActive(int neuronId) {
        flags[neuronId] &= 0b11011111;
    }

    public boolean isActive(int neuronId) {
        return (flags[neuronId] & 0b00100000) != 0;
    }
    
    public void setPendingFire(int neuronId) {
        flags[neuronId] |= 0b01000000;
    }

    public void clearPendingFire(int neuronId) {
        flags[neuronId] &= 0b10111111;
    }

    public boolean isPendingFire(int neuronId) {
        return (flags[neuronId] & 0b01000000) != 0;
    }
    
    public void setInhibitory(int neuronId) {
        flags[neuronId] |= 0b10000000;
    }

    public void clearInhibitory(int neuronId) {
        flags[neuronId] &= 0b01111111;
    }

    public boolean isInhibitory(int neuronId) {
        return (flags[neuronId] & 0b10000000) != 0;
    }
}