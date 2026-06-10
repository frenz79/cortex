package com.cortex.brain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.LayerConnectionsConfig;
import com.cortex.brain.layers.Layer;
import com.cortex.brain.layers.MultiLayer;
import com.cortex.brain.layers.SphericalLayer;
import com.cortex.commons.Point3f;

public class Brain extends MultiLayer<SphericalLayer>{

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	private static final int CLASSIFIERS_TARGET_LAYER = 4;

	private static final int SENSORS_TARGET_LAYER = 0;

	// Single Neurons storage
	private volatile CorticalNeuron[] neurons;
	private int totalNeurons = 0;
	private CorticalNeuronFactory neuronFactory;
	private BrainLayersConnConfig brainLayersConnConfig;
	private final List<LayerConfig> layersConfigs = new ArrayList<>();
	private final List<LayerConnectionsConfig> layersConnConfigs = new ArrayList<>();

	public class CorticalNeuronFactory {

		private final List<LayerConfig> configs;
		private int counter = 0;

		public synchronized CorticalNeuron buildNeuron( 
				int layerId, 
				boolean inhibitor, 
				Point3f position
				) {
			CorticalNeuron n = new CorticalNeuron(
					counter, 
					layerId, 
					configs.get(layerId).HAS_INCOMING, 
					configs.get(layerId).HAS_OUTGOING, 
					configs.get(layerId).CORTICAL_NEURONS_CONFIG,
					inhibitor, 
					position,
					configs.size()
					);
			neurons[counter++] = n;
			return n;
		}

		public CorticalNeuronFactory(List<LayerConfig> configs) {
			super();
			this.configs = configs;
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}   

	public static class Builder {
		private final Brain brain;

		public Builder() {
			this.brain = new Brain();
		}

		public Builder addLayerConfig(LayerConfig cfg) {
			brain.layersConfigs.add(cfg);
			return this;
		}

		public Builder addLayerConnectionConfig(BrainLayersConnConfig cfg) {
			brain.brainLayersConnConfig = cfg;
			return this;
		}

		public Builder withTotalNeurons(int totalNeurons) {
			brain.totalNeurons = totalNeurons;
			return this;
		}	     

		private void validate() {

		}

		public Brain build() {
			validate();
			return brain.build();
		}
	}

	Brain() {
	}

	Brain build() {
		this.neurons = new CorticalNeuron[totalNeurons];
		this.neuronFactory = new CorticalNeuronFactory( this.layersConfigs );
		var layers = generateLayers(layersConfigs);
		this.layersConnConfig = this.brainLayersConnConfig.getLayersConnectionsConfig(layers.getAllLayers());
		generateConnections(this.layersConnConfig);
		return this;
	}

	public void compact() {
		logger.info("Compacting cortical neurons");
		for( CorticalNeuron n : this.neurons ) {
			n.compact();
		}
	}

	// Called by superclass
	@Override
	protected SphericalLayer buildLayer( LayerConfig cfg ) {
		SphericalLayer l = (SphericalLayer) new SphericalLayer(cfg)
				.populate( this.neuronFactory )
				.connectInternal();
		return l;
	}

	/**
	 * Iterate active neurons and call consumer. Consumer returns true to keep active flag true,
	 * false to clear it. This method is safe to call concurrently.
	 */
	public void streamActiveNeuron(Function<CorticalNeuron, Boolean> consumer) {
		CorticalNeuron[] snapshot = neurons; // volatile read
		if (snapshot == null) return;
		Arrays.stream(snapshot).parallel().forEach( n -> {
			if (n != null && n.isActive()) {
				boolean stay = true;
				try {
					stay = consumer.apply(n);
				} catch (RuntimeException ex) {
					logger.error("Handled Exception:", ex);
					stay = true;
				}
				n.setActive( stay );
			} 
		});
	}

	public Layer getSensorsTargetLayer() {
		return getLayer(SENSORS_TARGET_LAYER);
	}

	public Layer getClassifiersSourceLayer() {
		return getLayer(CLASSIFIERS_TARGET_LAYER);
	}

	public AbstractNeuron[] getAllNeurons() {
		return neurons;
	}
}
