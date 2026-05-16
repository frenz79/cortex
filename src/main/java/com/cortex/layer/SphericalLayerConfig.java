package com.cortex.layer;

import java.util.function.Predicate;

import com.cortex.base.Neuron;

public class SphericalLayerConfig extends LayerConfig {

	private final float radius;
	
	public SphericalLayerConfig(
			int neurons, 
			float inhibitorFreq, 
			int minConnections, 
			int maxConnections, 
			float maxConnDistance, 
			float radius, 
			boolean hasIncoming, 
			boolean hasOutgoing, 
			Predicate<Neuron> connectionFilter, 
			SynapsePlasticityConfig synapsePlasticityConfig) {
		super(neurons, 
			inhibitorFreq, 
			minConnections, 
			maxConnections, 
			maxConnDistance, 
			hasIncoming,
			hasOutgoing,
			connectionFilter, 
			synapsePlasticityConfig);
		this.radius = radius;
	}

	public float getRadius() {
		return radius;
	}

}
