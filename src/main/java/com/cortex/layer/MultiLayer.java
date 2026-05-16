package com.cortex.layer;

import java.util.ArrayList;
import java.util.List;

public abstract class MultiLayer<MC extends MultiLayerConfig<C>, C extends LayerConfig, L extends Layer<C>> {

	private final MultiLayerConfig<C> config;
	protected List<L> layers;

	public abstract L buildLayer( C config );
	public abstract int getNeuronsCount();
	public abstract int getSynapsesCount();
	
	public MultiLayer(MultiLayerConfig<C> config) {
		super();
		this.config = config;
		buildLayers();
		connectLayers();
	}
	
	private void buildLayers() {
		this.layers = new ArrayList<L>(config.getNumberOfLayers());
		for ( C config : config.getConfigs() ) {
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
			
			System.out.println("L"+srcLayer.getId()+" -> L"+dstLayer.getId()+" : created "+connections+" synapses");
		});
	}
	
	public List<L> getAllLayers() {
		return layers;
	}
	
	public L getLayers(int index) {
		return layers.get(index);
	}

	public MultiLayerConfig<C> getConfig() {
		return config;
	}
}
