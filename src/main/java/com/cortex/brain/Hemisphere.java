package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.SynapsesBuilder;
import com.cortex.base.externals.IActuator;
import com.cortex.base.externals.IClassifier;
import com.cortex.base.externals.ISensor;
import com.cortex.base.externals.ISupervisor;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.LayerConfig;
import com.cortex.base.layers.LayerConnConfig;
import com.cortex.base.layers.MultiLayersConnConfig;
import com.cortex.base.layers.SphericalLayer;
import com.cortex.base.soa.NeuronStateSoA;
import com.cortex.base.utils.Point3f;

public class Hemisphere<L extends Abstract3DLayer> {

	final Logger logger = LogManager.getLogger(this.getClass());
	
	private static final int CLASSIFIERS_TARGET_LAYER = 4;
	private static final int SENSORS_TARGET_LAYER = 0;
	
	protected List<L> layers = new ArrayList<>();
	
	// Single Neurons storage
	private volatile CorticalNeuron[] neurons;
	private volatile NeuronStateSoA neuronsStatesBuff;
		
	private final int hemisphereId;
	private int totalNeurons = 0;
	private CorticalNeuronFactory neuronFactory;
	private MultiLayersConnConfig multiLayersConnConfig;
	private final List<LayerConfig> layersConfigs = new ArrayList<>();
	private final List<LayerConnConfig> layersConnConfigs = new ArrayList<>();
	
	private final List<ISensor> sensors;
	private final List<IActuator> actuators;
	private final List<IClassifier<?>> classifiers;
	private final List<ISupervisor<?>> supervisors;
	
	public class CorticalNeuronFactory {

		private final List<LayerConfig> configs;
		private int counter = 0;

		public synchronized CorticalNeuron buildNeuron( 
				Abstract3DLayer layer, 
				boolean inhibitor, 
				Point3f position
				) {
			CorticalNeuron n = new CorticalNeuron(
					counter, 
					hemisphereId,
					layer, 
					configs.get(layer.getLayerId()).HAS_INCOMING, 
					configs.get(layer.getLayerId()).HAS_OUTGOING, 
					configs.get(layer.getLayerId()).CORTICAL_NEURONS_CONFIG,
					inhibitor, 
					position
					);
			neurons[counter++] = n;
			return n;
		}

		public CorticalNeuronFactory(List<LayerConfig> configs) {
			super();
			this.configs = configs;
		}
	}
	
	public Hemisphere( int hemisphereId ) {
		this.hemisphereId = hemisphereId;
		// CopyOnWriteArrayList is ideal when attaches are rare and reads are frequent
		this.sensors = new CopyOnWriteArrayList<>();
		this.actuators = new CopyOnWriteArrayList<>();
		this.classifiers = new CopyOnWriteArrayList<>();
		this.supervisors = new CopyOnWriteArrayList<>();
	}
	
	public Hemisphere<L> build() {
		// Allocate neurons space
		this.neurons = new CorticalNeuron[totalNeurons];
		this.neuronsStatesBuff = new NeuronStateSoA(totalNeurons);
				    
		this.neuronFactory = new CorticalNeuronFactory( this.layersConfigs );
		// Generate and populate layers
		generateLayers(layersConfigs);
		// Generate synapses
		generateConnections();
		return this;
	}
	
	private void generateLayers( List<LayerConfig> configs ) {
		Map<Integer,L> layersBld = new ConcurrentHashMap<Integer, L>();
		
		configs.parallelStream().forEach( c -> {
			layersBld.put(c.getLayerId(), buildLayer(c));
		});
		
		for (int i=0; i<configs.size(); i++) {
			this.layers.add( layersBld.get(i) );
		}
	}
	
	private L buildLayer( LayerConfig cfg ) {
		SphericalLayer l = (SphericalLayer) new SphericalLayer(cfg)
				.populate( this.neuronFactory );
		return (L) l;
	}
	
	private void generateConnections() {
		SynapsesBuilder bld = new SynapsesBuilder( this.neurons );
		logger.info("Generating internal layer connections");
		layers.parallelStream().forEach( l -> {
			bld.buildInternalSynapses( l );
		});
		
		logger.info("Generating layer to layer connections");
		var layersConnConfig = this.multiLayersConnConfig.getLayersConnectionsConfig(layers);
		bld.buildLayersSynapses(layersConnConfig);
		
		logger.info("Generating external synapses to layer connections");
		for ( ISensor s : sensors ) {
			int synapses = bld.buildExternalSynapses( s, layers.get(s.getExternalConnConfig().LINKED_LAYER_ID) );
			logger.info("ISensor:{} -> L{} : created {} synapses", 
				s.getId(), s.getExternalConnConfig().LINKED_LAYER_ID, synapses);
		}		
		
		bld.build();
	}
	
	public void process(long now) {
		if (neurons == null) return;
		CorticalNeuron[] snapshot = neurons;
	    
		/*
	    // PHASE 1 — Spike propagation
	    Arrays.stream(snapshot).parallel().forEach(n -> {
	        if (n.isPendingFire()) {
	            n.delayedFire(now);
	        }
	    });

	    // PHASE 2 — Integration
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
	    */
		
		// Faster iteration
		int N = totalNeurons;
		IntStream.range(0, N).parallel().forEach(i -> {
		    CorticalNeuron n = neurons[i];
		    if (neuronsStatesBuff.pendingFire[i]) {
		        n.delayedFire(now);
		    }
		});

		IntStream.range(0, N).parallel().forEach(i -> {
		    CorticalNeuron n = neurons[i];
		    if (neuronsStatesBuff.isActive[i]) {
		        boolean stay = false;
				try {
					stay = n.process(now);
				} catch (InterruptedException ex) {
					logger.error("Handled Exception:", ex);
				}
		        neuronsStatesBuff.isActive[i] = stay;
		    }
		});
	}
	
	public Abstract3DLayer getSensorsTargetLayer() {
		return getLayer(SENSORS_TARGET_LAYER);
	}

	public Abstract3DLayer getClassifiersSourceLayer() {
		return getLayer(CLASSIFIERS_TARGET_LAYER);
	}

	public AbstractNeuron[] getAllNeurons() {
		return neurons;
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
	
	public List<L> getAllLayers() {
		return layers;
	}
	
	public L getLayer(int index) {
		return layers.get(index);
	}
	
	public static Builder newBuilder( int hemisphereId ) {
		return new Builder( hemisphereId );
	}   

	public static class Builder<L extends Abstract3DLayer> {
		final Logger logger = LogManager.getLogger(this.getClass());
		private final Hemisphere<L> emisphere;

		public Builder( int hemisphereId ) {
			this.emisphere = new Hemisphere<>( hemisphereId );
		}

		public Builder<L> addLayerConfig(LayerConfig cfg) {
			emisphere.layersConfigs.add(cfg);
			return this;
		}

		public Builder<L> addMultiLayersConnConfig(MultiLayersConnConfig cfg) {
			emisphere.multiLayersConnConfig = cfg;
			return this;
		}

		public Builder<L> withTotalNeurons(int totalNeurons) {
			emisphere.totalNeurons = totalNeurons;
			return this;
		}	
		
		public Builder<L>  attachSensor(ISensor s) {
			logger.info("Sensor attached:{}",s );
			emisphere.sensors.add(Objects.requireNonNull(s));	
			return this;
		}

		public Builder<L>  attachActuator(IActuator a) {
			logger.info("Actuator attached:{}",a );
			emisphere.actuators.add(Objects.requireNonNull(a));
			return this;
		}

		public Builder<L>  attachClassifier(IClassifier<?> c) {
			logger.info("Classifier attached:{}",c );
			emisphere.classifiers.add(Objects.requireNonNull(c));
			return this;
		}

		public Builder<L>  attachSupervisor(ISupervisor<?> s) {
			logger.info("Supervisor attached:{}", s );
			emisphere.supervisors.add(Objects.requireNonNull(s));
			return this;
		}

		private void validate() {

		}

		public Hemisphere<L> build() {
			validate();
			return emisphere.build();
		}
	}
}
