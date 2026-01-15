package com.cortex.layer;

import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;

public class SphericalLayerConfig extends LayerConfig {

	private final float radius;
	
	public SphericalLayerConfig(int neurons, float inhibitorFreq, int minConnections, int maxConnections, float maxConnDistance, float radius, Predicate<AbstractNeuron> connectionFilter, SynapsePlasticityConfig synapsePlasticityConfig) {
		super(neurons, inhibitorFreq, minConnections, maxConnections, maxConnDistance, connectionFilter, synapsePlasticityConfig);
		this.radius = radius;
	}

	public float getRadius() {
		return radius;
	}

}
