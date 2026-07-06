package com.cortex.externals;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public class ExtNeuronSoA {

	@SerializableAttribute
	public final float[] firingRate;
	@SerializableAttribute
	public final long[]  lastRateUpdate;
	@SerializableAttribute
	public final long[]  lastSpikeTime;
	@SerializableAttribute
	public final long[]  lastProcessTime;

	// bits 0..4  → 
	// bit  5     → isActive
	// bit  6     → pendingFire
	// bit  7     → inhibitory
	@SerializableAttribute
	public final byte[] flags;

	public final int totalNeurons;

	public ExtNeuronSoA(int totalNeurons) {
		this.firingRate = new float[totalNeurons];
		this.lastRateUpdate = new long[totalNeurons];
		this.lastSpikeTime = new long[totalNeurons];
		this.lastProcessTime = new long[totalNeurons];
		this.flags = new byte[totalNeurons];
		this.totalNeurons = totalNeurons;
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

	public boolean hasPendingFire(int neuronId) {
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