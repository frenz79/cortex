package com.cortex.brain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.modules.IActuator;
import com.cortex.base.modules.IClassifier;
import com.cortex.base.modules.ISensor;
import com.cortex.base.modules.ISupervisor;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.layers.Layer;
import com.cortex.brain.layers.LayerConfig;
import com.cortex.brain.layers.MultiLayer;
import com.cortex.brain.layers.SphericalLayer;

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
	
	private final List<ISensor> sensors;
	private final List<IActuator> actuators;
	private final List<IClassifier<?>> classifiers;
	private final List<ISupervisor<?>> supervisors;
	
	public void processAllActiveNeurons(long now) {
		if (neurons == null) return;
		CorticalNeuron[] snapshot = neurons;
	    
	    // PHASE 1 — Propagazione spike
	    Arrays.stream(snapshot).parallel().forEach(n -> {
	        if (n.isPendingFire()) {
	            n.delayedFire(now);
	        }
	    });

	    // PHASE 2 — Integrazione
	    Arrays.stream(snapshot).parallel().forEach(n -> {
	        if (n.isActive()) {
	            boolean stay = false;
				try {
					stay = n.process(now);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
	            n.setActive(stay);
	        }
	    });
	}

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
		// CopyOnWriteArrayList is ideal when attaches are rare and reads are frequent
		this.sensors = new CopyOnWriteArrayList<>();
		this.actuators = new CopyOnWriteArrayList<>();
		this.classifiers = new CopyOnWriteArrayList<>();
		this.supervisors = new CopyOnWriteArrayList<>();
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
		for( AbstractNeuron n : this.neurons ) {
			n.compact();
		}
		for (ISensor s : sensors ) {
			for( AbstractNeuron[] n1 : s.getNeurons() ) {
				for( AbstractNeuron n : n1 ) {
					n.compact();
				}
			}
		}
		for (IClassifier<?> s : classifiers ) {
			for( AbstractNeuron[] n1 : s.getNeurons() ) {
				for( AbstractNeuron n : n1 ) {
					n.compact();
				}
			}
		}
		for (IActuator s : actuators ) {
			for( AbstractNeuron[] n1 : s.getNeurons() ) {
				for( AbstractNeuron n : n1 ) {
					n.compact();
				}
			}
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
	
	public Layer getSensorsTargetLayer() {
		return getLayer(SENSORS_TARGET_LAYER);
	}

	public Layer getClassifiersSourceLayer() {
		return getLayer(CLASSIFIERS_TARGET_LAYER);
	}

	public AbstractNeuron[] getAllNeurons() {
		return neurons;
	}
	
	public void attachSensor(ISensor s) {
		logger.info("Sensor attached:{}",s );
		this.sensors.add(Objects.requireNonNull(s));
	}

	public void attachActuator(IActuator a) {
		logger.info("Actuator attached:{}",a );
		this.actuators.add(Objects.requireNonNull(a));
	}

	public void attachClassifier(IClassifier<?> c) {
		logger.info("Classifier attached:{}",c );
		this.classifiers.add(Objects.requireNonNull(c));
	}

	public void attachSupervisor(ISupervisor<?> s) {
		logger.info("Supervisor attached:{}", s );
		this.supervisors.add(Objects.requireNonNull(s));
	}

	public List<ISensor> getSensors() {
		return sensors;
	}

	public List<IActuator> getActuators() {
		return actuators;
	}

	public List<IClassifier<?>> getClassifiers() {
		return classifiers;
	}

	public List<ISupervisor<?>> getSupervisors() {
		return supervisors;
	}
}
