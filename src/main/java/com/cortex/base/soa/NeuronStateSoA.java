package com.cortex.base.soa;

// Hot fields grouped together for better locality
public final class NeuronStateSoA {

	public NeuronStateSoA(int totalNeurons) {
		this.firingRate = new float[totalNeurons];
		this.lastRateUpdate = new long[totalNeurons];
		this.isActive = new boolean[totalNeurons];
		this.pendingFire = new boolean[totalNeurons];
	}

	public float[] firingRate;
	public long[] lastRateUpdate;
	public boolean[] isActive;
	public boolean[] pendingFire;
}