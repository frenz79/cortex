package com.cortex.layer;

public class SphericalLayerConfig extends LayerConfig {

	private final float radius;
	
	public SphericalLayerConfig(int neurons, float inhibitorFreq, int minConnections, int maxConnections, float maxConnDistance, float radius) {
		super(neurons, inhibitorFreq, minConnections, maxConnections, maxConnDistance);
		this.radius = radius;
	}

	public float getRadius() {
		return radius;
	}

}
