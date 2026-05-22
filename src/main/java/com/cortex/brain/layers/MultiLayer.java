package com.cortex.brain.layers;

import java.util.ArrayList;
import java.util.List;

import com.cortex.base.config.LayerConfig;

public abstract class MultiLayer<L extends Layer> {

	private final MultiLayerConfig config;
	protected List<L> layers;

	public abstract L buildLayer( LayerConfig config );
	
	public MultiLayer(MultiLayerConfig config) {
		super();
		this.config = config;
	}
	
	public void buildAndConnectLayers() {
		buildLayers();
		connectLayers();
	}
	
	private void buildLayers() {
		this.layers = new ArrayList<L>(config.getNumberOfLayers());
		for ( LayerConfig config : config.getConfigs() ) {
			this.layers.add(buildLayer(config));
		}
	}
	
	private void connectLayers() {
		//for ( Entry<IntPair, IntraLayersConnConfig> e : config.getLayer2layerConns().entrySet() ) 
		
		config.getLayer2layerConns().entrySet().parallelStream().forEach(e -> {
		
			L srcLayer = this.layers.get( e.getKey().left() );
			L dstLayer = this.layers.get( e.getKey().right() );
			int connections = 0;
			
			connections += dstLayer.link(
				srcLayer, 
				e.getValue().minConnections(), 
				e.getValue().maxConnections(),
				e.getValue().maxDistance(), 
				e.getValue().filter(),
				e.getValue().synapsePlasticityConfig()
			);
			
			System.out.println("L"+srcLayer.getLayerId()+" -> L"+dstLayer.getLayerId()+" : created "+connections+" synapses");
		});
	}
	
	public List<L> getAllLayers() {
		return layers;
	}
	
	public L getLayers(int index) {
		return layers.get(index);
	}

	public MultiLayerConfig getConfig() {
		return config;
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
