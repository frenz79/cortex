package com.cortex.brain.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.CorticalNeuron;
import com.cortex.commons.IntPair;

public class MultiLayerConfig {
	
	private final Map<IntPair,IntraLayersConnConfig> layer2layerConns = new HashMap<>();
	
	public static record IntraLayersConnConfig(int minConnections, int maxConnections, float maxDistance, SynapsePlasticityConfig synapsePlasticityConfig, Predicate<CorticalNeuron> filter) {
	}
	
	private final List<LayerConfig> configs = new ArrayList<>();
		
	public MultiLayerConfig addLayerConfig( LayerConfig cfg ){
		this.configs.add(cfg);
		return this;
	}
	
	public MultiLayerConfig addIntraLayerConfig( int fromLayer, int toLayer, IntraLayersConnConfig cfg ){
		this.layer2layerConns.put( new IntPair(fromLayer, toLayer), cfg );
		return this;
	}
	
	public IntraLayersConnConfig getIntraLayerConfig( int fromLayer, int toLayer ){
		return this.layer2layerConns.get( new IntPair(fromLayer, toLayer));
	}
	
	public List<LayerConfig> getConfigs() {
		return configs;
	}
	
	public int getNumberOfLayers() {
		return configs.size();
	}

	public Map<IntPair, IntraLayersConnConfig> getLayer2layerConns() {
		return layer2layerConns;
	}
}
