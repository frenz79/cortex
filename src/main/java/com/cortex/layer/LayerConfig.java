package com.cortex.layer;

import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;

public class LayerConfig {
	private final int neurons;
	private final float inhibitorFreq;
	private final int minConnections;
	private final int maxConnections;
	private final float maxConnDistance;
	private final Predicate<AbstractNeuron> connectionFilter;
	private final SynapsePlasticityConfig synapsePlasticityConfig;
	
	public LayerConfig(int neurons, float inhibitorFreq, int minConnections, int maxConnections, float maxConnDistance, Predicate<AbstractNeuron> connectionFilter, SynapsePlasticityConfig synapsePlasticityConfig) {
		super();
		this.neurons = neurons;
		this.inhibitorFreq = inhibitorFreq;
		this.minConnections = minConnections;
		this.maxConnections = maxConnections;
		this.maxConnDistance = maxConnDistance;
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

	public Predicate<AbstractNeuron> getConnectionFilter() {
		return connectionFilter;
	}

	public SynapsePlasticityConfig getSynapsePlasticityConfig() {
		return synapsePlasticityConfig;
	}

}
