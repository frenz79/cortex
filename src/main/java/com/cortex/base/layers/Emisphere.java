package com.cortex.base.layers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.SynapsesBuilder;
import com.cortex.base.modules.IActuator;
import com.cortex.base.modules.IClassifier;
import com.cortex.base.modules.ISensor;
import com.cortex.base.modules.ISupervisor;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.BrainLayersConnConfig;
import com.cortex.brain.CorticalNeuron;
import com.cortex.brain.LayerConnectionsConfig;

public class Emisphere<L extends Abstract3DLayer> {

	final Logger logger = LogManager.getLogger(this.getClass());
	
	private static final int CLASSIFIERS_TARGET_LAYER = 4;
	private static final int SENSORS_TARGET_LAYER = 0;
	
	protected List<L> layers = new ArrayList<>();
	protected List<LayerConnectionsConfig>	layersConnConfig = new ArrayList<>();
	
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
	
	public Emisphere() {
		// CopyOnWriteArrayList is ideal when attaches are rare and reads are frequent
		this.sensors = new CopyOnWriteArrayList<>();
		this.actuators = new CopyOnWriteArrayList<>();
		this.classifiers = new CopyOnWriteArrayList<>();
		this.supervisors = new CopyOnWriteArrayList<>();
	}
	
	public Emisphere<L> build() {
		// Allocate neurons space
		this.neurons = new CorticalNeuron[totalNeurons];
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
	
	private void generateConnections() {
		this.layersConnConfig = this.brainLayersConnConfig.getLayersConnectionsConfig(layers);
		new SynapsesBuilder( )
			.generateConnections(layersConnConfigs);
		
	}
	
	public void process(long now) {
		if (neurons == null) return;
		CorticalNeuron[] snapshot = neurons;
	    
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
	}
		
	protected L buildLayer( LayerConfig cfg ) {
		SphericalLayer l = (SphericalLayer) new SphericalLayer(cfg)
				.populate( this.neuronFactory )
				.connectInternal();
		return (L) l;
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
