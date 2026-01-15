package com.cortex.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.cortex.base.AbstractNeuron;
import com.cortex.commons.IntPair;
import com.cortex.layer.MultiLayerConfig.IntraLayersConnConfig;

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
	
	private float getInhibProbability( int l1, int l2 ) {
		// Intra-layer
		if ( l1==l2 ) {
			return 0.35f;
		}
		int delta = l1-l2;
		
		// Feed forward
		if (delta==-1) {
			return 0.20f;
		}
		// Feedback
		if (delta==1) {
			return 0.80f;
		}
		// Long range
		return 0.90f;
	}
	
	private void connectLayers() {
		for ( Entry<IntPair, IntraLayersConnConfig> e : config.getLayer2layerConns().entrySet() ) {
			L srcLayer = this.layers.get( e.getKey().left() );
			L dstLayer = this.layers.get( e.getKey().right() );
			int connections = 0;
			
				connections += dstLayer.connectExternalLayer(
					srcLayer.getNeurons(), 
					e.getValue().minConnections(), 
					e.getValue().maxConnections(),
					e.getValue().maxDistance(), 
					true,
					e.getValue().filter(),
					e.getValue().synapsePlasticityConfig()
				);
				//	System.out.println("L"+srcLayer.getId()+" N["+srcNeuron.getPosition()+"]-> "+dstNeurons.size()+" synapses");	
			System.out.println("L"+srcLayer.getId()+" -> L"+dstLayer.getId()+" : created "+connections+" synapses");
		}
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
