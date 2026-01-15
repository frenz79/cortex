package com.cortex.layer;

public class LayerConfig {
	private final int neurons;
	private final float inhibitorFreq;
	private final int minConnections;
	private final int maxConnections;
	private final float maxConnDistance;
	
	public LayerConfig(int neurons, float inhibitorFreq, int minConnections, int maxConnections, float maxConnDistance) {
		super();
		this.neurons = neurons;
		this.inhibitorFreq = inhibitorFreq;
		this.minConnections = minConnections;
		this.maxConnections = maxConnections;
		this.maxConnDistance = maxConnDistance;
	}
	
	public int getNeurons() {
		return neurons;
	}
	
	public float getInhibitorFreq() {
		return inhibitorFreq;
	}

	public int getMinConnections() {
		return minConnections;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public float getMaxConnDistance() {
		return maxConnDistance;
	}

}
