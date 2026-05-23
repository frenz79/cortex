package com.cortex.brain.layers;

import java.util.ArrayList;
import java.util.List;

import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.LayerConnectionsConfig;

public abstract class MultiLayer<L extends Layer> {

	protected List<L> layers = new ArrayList<>();
	protected List<LayerConnectionsConfig>	layersConnConfig = new ArrayList<>();
	
	protected abstract L buildLayer( LayerConfig config );
	
	public MultiLayer() {
		super();
	}
	
	public MultiLayer<L> generateLayers( List<LayerConfig> configs ) {
		for ( LayerConfig c : configs ) {
			this.layers.add(c.getLayerId(),	buildLayer(c) );
		}
		return this;
	}
		
	public void generateConnections( List<LayerConnectionsConfig> configs ) {
		configs.parallelStream().forEach(e -> {
			
			Layer srcLayer = e.SOURCE_LAYER;
			Layer dstLayer = e.TARGET_LAYER;
			int connections = 0;
			
			connections += dstLayer.link(
				srcLayer, 
				e.MIN_CONNECTIONS, 
				e.MAX_CONNECTIONS,
				e.MAX_DISTANCE, 
				e.NEURON_FILTER_PREDICATE,
				e.SYNAPSE_PLASTICITY_CONFIG
			);
			
			System.out.println("L"+srcLayer.getLayerId()+" -> L"+dstLayer.getLayerId()+" : created "+connections+" synapses");
		});
	}
	
	public List<L> getAllLayers() {
		return layers;
	}
	
	public L getLayer(int index) {
		return layers.get(index);
	}

	public int getNeuronsCount() {
		int ret = 0;
		for ( L l : layers ) {
			ret += l.getNeuronsCount();
		}
		return ret;
	}

	public int getSynapsesCount() {
		int ret = 0;
		for ( L l : layers ) {
			ret += l.getSynapsesCount();
		}
		return ret;
	}	
}
