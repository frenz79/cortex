package com.cortex.layer;

import java.util.function.Predicate;

import com.cortex.base.Neuron;

public class LayerConfig {
	private final int neurons;
	private final float inhibitorFreq;
	private final int minConnections;
	private final int maxConnections;
	private final boolean hasIncoming;
	private final boolean hasOutgoing;
	private final float maxConnDistance;
	private final Predicate<Neuron> connectionFilter;
	private final SynapsePlasticityConfig synapsePlasticityConfig;
	
	public LayerConfig(
			int neurons, 
			float inhibitorFreq, 
			int minConnections, 
			int maxConnections, 
			float maxConnDistance, 
			boolean hasIncoming, 
			boolean hasOutgoing, 
			Predicate<Neuron> connectionFilter, 
			SynapsePlasticityConfig synapsePlasticityConfig) {
		super();
		this.neurons = neurons;
		this.inhibitorFreq = inhibitorFreq;
		this.minConnections = minConnections;
		this.maxConnections = maxConnections;
		this.maxConnDistance = maxConnDistance;
		this.hasIncoming = hasIncoming;
		this.hasOutgoing = hasOutgoing;
		this.connectionFilter = connectionFilter;
		this.synapsePlasticityConfig = synapsePlasticityConfig;
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

	public Predicate<Neuron> getConnectionFilter() {
		return connectionFilter;
	}

	public SynapsePlasticityConfig getSynapsePlasticityConfig() {
		return synapsePlasticityConfig;
	}

	public boolean isHasIncoming() {
		return hasIncoming;
	}

	public boolean isHasOutgoing() {
		return hasOutgoing;
	}

}
