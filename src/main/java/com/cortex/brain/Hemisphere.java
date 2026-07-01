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

import com.cortex.base.beans.NeuronBean;
import com.cortex.base.builders.SoABuilder;
import com.cortex.base.builders.SoABuilder.SoABuilderResult;
import com.cortex.base.builders.SynapsesBuilder;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.LayerConfig;
import com.cortex.base.layers.LayerConnConfig;
import com.cortex.base.layers.MultiLayersConfig;
import com.cortex.base.layers.MultiLayersConnConfig;
import com.cortex.base.layers.SphericalLayer;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.base.soa.DendriticCompetitionParamsSoA;
import com.cortex.base.soa.DendriticTreeSoA;
import com.cortex.base.soa.LateralInhibitionLayerSoA;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.NeuronTopologySoA;
import com.cortex.base.soa.PlasticityParamsSoA;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.soa.SpatialHashSoA;
import com.cortex.base.soa.SpikeBufferSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.soa.constants.BranchTypeCode;
import com.cortex.base.soa.logic.CombinedLateralInhibitionLogic;
import com.cortex.base.soa.logic.CorticalNeuronLogic;
import com.cortex.base.soa.logic.DendriticCompetitionLogic;
import com.cortex.base.soa.logic.ExcitatoryPlasticityLogic;
import com.cortex.base.soa.logic.InhibitoryPlasticityLogic;
import com.cortex.base.soa.logic.SpikeRingBufferLogic;
import com.cortex.base.soa.logic.SynapseBranchLogic;
import com.cortex.base.soa.logic.SynapseLogic;

public class Hemisphere<L extends Abstract3DLayer> {

	final Logger logger = LogManager.getLogger(this.getClass());
	
	private static final int CLASSIFIERS_TARGET_LAYER = 4;
	private static final int SENSORS_TARGET_LAYER = 0;
	
	protected List<L> layers = new ArrayList<>();
		
	private final int hemisphereId;
	private int totalNeurons = 0;
	private int totalSynapses = 0;
	
	private MultiLayersConnConfig multiLayersConnConfig;
	private final List<LayerConfig> layersConfigs = new ArrayList<>();
	private final List<LayerConnConfig> layersConnConfigs = new ArrayList<>();
	/*
	private final List<ISensor> sensors;
	private final List<IActuator> actuators;
	private final List<IClassifier<?>> classifiers;
	private final List<ISupervisor<?>> supervisors;
	*/	
	public Hemisphere( int hemisphereId ) {
		this.hemisphereId = hemisphereId;
		// CopyOnWriteArrayList is ideal when attaches are rare and reads are frequent
		/*
		this.sensors = new CopyOnWriteArrayList<>();
		this.actuators = new CopyOnWriteArrayList<>();
		this.classifiers = new CopyOnWriteArrayList<>();
		this.supervisors = new CopyOnWriteArrayList<>();
		*/
	}
	
	private volatile NeuronBean[] hemisphereNeurons;
	
	public Hemisphere<L> build(	MultiLayersConfig layersCfg	) throws Exception {
		hemisphereNeurons = new NeuronBean[totalNeurons];
		// Generate and populate layers
		generateLayers(layersConfigs);
		// Generate synapses
		generateConnections();
		
		// Generate SoA		
		//CorticalNeuronsConfig corticalNeuronsConfig = layersCfg.
		//ExcitatorySynapticPlasticityConfig excitatorySynapticPlasticityConfig,
		//InhibitorySynapticPlasticityConfig inhibitorySynapticPlasticityConfig 
		
		generateSoA(
		);
		return this;
	}
	
	private NeuronSoA neuronSoA;
	private SynapseSoA synapseSoA;
	private SynapseBranchSoA synapseBranchSoA;
	private SynapseTopologySoA synapseTopologySoA;
	private DendriticTreeSoA dendriticTreeSoA;
	private PlasticitySoA plasticitySoA;
	private SpikeBufferSoA spikeBuffer;
	private LateralInhibitionLayerSoA lateralInhibitionLayerSoA;
	private DendriticCompetitionParamsSoA dendriticCompetitionParamsSoA;
	private PlasticityParamsSoA plasticityParamsSoA;
	
	private ExcitatoryPlasticityLogic excitatoryPlasticityLogic;
	private InhibitoryPlasticityLogic inhibitoryPlasticityLogic;
	private NeuronTopologySoA neuronTopologySoA;
	private CorticalNeuronLogic corticalNeuronLogic;
	private SynapseLogic synapseLogic;
	private SynapseBranchLogic synapseBranchLogic;
	private CombinedLateralInhibitionLogic combinedLateralInhibitionLogic;
	private DendriticCompetitionLogic dendriticCompetitionLogic;
	private SpikeRingBufferLogic spikeBufferLogic;

	private SpatialHashSoA spatialHashSoA;
	
	private void generateSoA( 
	//	CorticalNeuronsConfig corticalNeuronsConfig,
	//	ExcitatorySynapticPlasticityConfig excitatorySynapticPlasticityConfig,
	//	InhibitorySynapticPlasticityConfig inhibitorySynapticPlasticityConfig 
	) throws Exception {
		logger.info("Generating SOA Modules");
		
		SoABuilder bld = new SoABuilder( );
		SoABuilderResult result = bld.buildAll(hemisphereNeurons);
		this.neuronSoA = result.neuronSoA();
		this.synapseSoA = result.synapseSoA();
		
		this.totalSynapses = this.synapseSoA.totalSynapses;
		
		this.synapseBranchSoA = result.synapseBranchSoA();
		this.synapseTopologySoA = result.synapseTopologySoA();
		this.dendriticTreeSoA = result.dendriticTreeSoA();
		this.neuronTopologySoA = result.neuronTopologySoA();
		
		this.plasticitySoA = new PlasticitySoA(this.synapseSoA.totalSynapses);
		this.plasticityParamsSoA = buildPlasticityParamsSoA( this.synapseSoA );
		
		this.dendriticCompetitionParamsSoA = new DendriticCompetitionParamsSoA(BranchTypeCode.size());
		this.spatialHashSoA = buildSpatialHashSoA(hemisphereNeurons, neuronSoA, layersConfigs);
		
		// TODO: fill SOA!
		
		logger.info("Generating SOA Logic Modules");
		
		this.dendriticCompetitionLogic = new DendriticCompetitionLogic(
			this.synapseBranchSoA,
			this.dendriticTreeSoA,
			this.dendriticCompetitionParamsSoA
		);
		
		this.excitatoryPlasticityLogic = new ExcitatoryPlasticityLogic( this.plasticitySoA );
		
		this.inhibitoryPlasticityLogic = new InhibitoryPlasticityLogic( this.plasticitySoA );
		
		this.spikeBufferLogic = new SpikeRingBufferLogic(
			4096,
			256,
			10_000_000l,
			this.synapseSoA,
			this.synapseBranchSoA
		);
		
		this.synapseLogic = new SynapseLogic(
			this.excitatoryPlasticityLogic,
			this.inhibitoryPlasticityLogic,	
			this.synapseSoA,
			this.plasticitySoA,
			this.plasticityParamsSoA
		);
		
		this.synapseBranchLogic = new SynapseBranchLogic(
			this.synapseBranchSoA,
			this.synapseSoA,
			this.synapseTopologySoA,
			this.spikeBufferLogic
		);
		
		this.corticalNeuronLogic = new CorticalNeuronLogic(
			getCorticalNeuronsConfigs(layersConfigs),
			this.synapseBranchLogic,
			this.dendriticCompetitionLogic,
			
			this.neuronSoA,
			this.neuronTopologySoA,
			this.synapseSoA,
			this.synapseBranchSoA,
			this.synapseTopologySoA,
			this.synapseLogic,
			this.spikeBufferLogic,
			this.combinedLateralInhibitionLogic			
		); 
		
		this.spatialHashSoA = buildSpatialHashSoA(
			hemisphereNeurons,
			this.neuronSoA,
			layersConfigs
		);
	}
	
	private PlasticityParamsSoA buildPlasticityParamsSoA( SynapseSoA synapseSoA ) {
		PlasticityParamsSoA ret = new PlasticityParamsSoA(synapseSoA.totalSynapses);
		InhibitorySynapticPlasticityConfig[] inhCfg = getInhibitorySynapticPlasticityConfigs(layersConfigs);
		ExcitatorySynapticPlasticityConfig[] excCfg = getExcitatorySynapticPlasticityConfigs(layersConfigs);
		
		for (int synId = 0; synId < synapseSoA.totalSynapses; synId++) {
		    int layerId = neuronSoA.getLayerId( synapseSoA.sourceNeuronId[synId] );
		    boolean excitatory = plasticitySoA.isExcitatory(synId);

		    if (excitatory) {
		        ExcitatorySynapticPlasticityConfig cfg = excCfg[layerId];

		        ret.tauPlusOrLearningRate[synId]        = cfg.TAU_PLUS;
		        ret.tauMinusOrTargetFiringRate[synId]   = cfg.TAU_MINUS;
		        ret.aPlusOrWMin[synId]                  = cfg.A_PLUS;
		        ret.aMinusOrWMax[synId]                 = cfg.A_MINUS;

		        ret.homeostaticRateOrLearningRate[synId] = cfg.HOMEOSTATIC_RATE;
		        ret.wBaselineOrTargetRate[synId]   		 = cfg.W_BASELINE;
		        ret.wMin[synId]                          = cfg.W_MIN;
		        ret.wMax[synId]                          = cfg.W_MAX;
		        ret.eligibilityDecayNanos[synId]         = cfg.ELIGIBILITY_DECAY_NANOS;

		    } else {
		        InhibitorySynapticPlasticityConfig cfg = inhCfg[layerId];

		        ret.tauPlusOrLearningRate[synId]        = cfg.LEARNING_RATE;
		        ret.tauMinusOrTargetFiringRate[synId]   = cfg.TARGET_FIRING_RATE;
		        ret.aPlusOrWMin[synId]                  = cfg.W_MIN;
		        ret.aMinusOrWMax[synId]                 = cfg.W_MAX;

		        ret.homeostaticRateOrLearningRate[synId] = cfg.LEARNING_RATE;
		        ret.wBaselineOrTargetRate[synId]   		 = cfg.TARGET_FIRING_RATE;
		        ret.wMin[synId]                          = cfg.W_MIN;
		        ret.wMax[synId]                          = cfg.W_MAX;
		        ret.eligibilityDecayNanos[synId]         = 0L;
		    }
		}		
		return ret;
	}
	
	
	private static final InhibitorySynapticPlasticityConfig[] getInhibitorySynapticPlasticityConfigs(List<LayerConfig> cfg) {
		InhibitorySynapticPlasticityConfig[] ret = new InhibitorySynapticPlasticityConfig[cfg.size()];
		for (int i=0; i<cfg.size(); i++) {
			ret[i] = cfg.get(i).SYNAPSE_PLASTICITY_CONFIG.inhibitory();
		}
		return ret;
	}

	private static final ExcitatorySynapticPlasticityConfig[] getExcitatorySynapticPlasticityConfigs(List<LayerConfig> cfg) {
		ExcitatorySynapticPlasticityConfig[] ret = new ExcitatorySynapticPlasticityConfig[cfg.size()];
		for (int i=0; i<cfg.size(); i++) {
			ret[i] = cfg.get(i).SYNAPSE_PLASTICITY_CONFIG.excitatory();
		}
		return ret;
	}

	private static final CorticalNeuronsConfig[] getCorticalNeuronsConfigs(List<LayerConfig> cfg) {
		CorticalNeuronsConfig[] ret = new CorticalNeuronsConfig[cfg.size()];
		for (int i=0; i<cfg.size(); i++) {
			ret[i] = cfg.get(i).CORTICAL_NEURONS_CONFIG;
		}
		return ret;
	}

	public SpatialHashSoA buildSpatialHashSoA(
	        NeuronBean[] neurons,
	        NeuronSoA neuronSoA,
	        List<LayerConfig> layerConfigs
	) {
	    int totalLayers = layerConfigs.size();
	    SpatialHashSoA ret = new SpatialHashSoA(totalLayers, neuronSoA);

	    for (int layer = 0; layer < totalLayers; layer++) {
	        LayerConfig cfg = layerConfigs.get(layer);
	        int N = cfg.NEURONS_COUNT;
	        float R = cfg.DIMENSION / 2.0f;
	        // Volume della sfera
	        float V = (float)((4.0 / 3.0) * Math.PI * R * R * R);
	        // densità neuroni/m^3
	        float rho = N / V;
	        // densità target: 8 neuroni per cella
	        float target = 8.0f;
	        // dimensione cella
	        float cellSize = (float)Math.cbrt(target / rho);
	        // celle per asse
	        int cells = (int)Math.ceil(cfg.DIMENSION / cellSize);
	        ret.initLayer(layer, cellSize, cells, cells, cells);
	    }

	    // Inserisci neuroni
	    for (int neuronId = 0; neuronId < neurons.length; neuronId++) {
	        ret.insertNeuron(neuronId);
	    }

	    return ret;
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
		SphericalLayer l = new SphericalLayer(cfg);
		l.populate(
			hemisphereNeurons,
			getStartNeuronsIndex(l.getLayerId()),
			l.getNeuronsCount()
		);
		return (L)l;
	}
	
	private int getStartNeuronsIndex(int layerId) {
		int offset = 0;
		if (layerId>0) {
			for(int i=0; i<layerId; i++) {
				offset += layersConfigs.get(i).NEURONS_COUNT;
			}
		}
		return offset;
	}

	private void generateConnections() {
		SynapsesBuilder bld = new SynapsesBuilder( this.hemisphereNeurons );
		logger.info("Generating internal layer connections");
		layers.parallelStream().forEach( l -> {
			bld.buildInternalSynapses( l );
		});
		
		logger.info("Generating layer to layer connections");
		var layersConnConfig = this.multiLayersConnConfig.getLayersConnectionsConfig(layers);
		bld.buildLayersSynapses(layersConnConfig);
		/*
		logger.info("Generating external synapses to layer connections");
		for ( ISensor s : sensors ) {
			int synapses = bld.buildExternalSynapses( s, layers.get(s.getExternalConnConfig().LINKED_LAYER_ID) );
			logger.info("ISensor:{} -> L{} : created {} synapses", 
				s.getId(), s.getExternalConnConfig().LINKED_LAYER_ID, synapses);
		}		
		*/
		bld.build();
	}
	
	public void process(long now) {
		// 1. PROCESS NEURONS (soma + dendriti + plasticità pre-spike)
		IntStream.range(0, neuronSoA.totalNeurons)
			.parallel()
			.forEach(i -> corticalNeuronLogic.process(now, i));
		
		// 2. PROCESS DELAYED FIRES (propagazione spike + plasticità post-spike)
		IntStream.range(0, neuronSoA.totalNeurons)
			.parallel()
			.forEach(i -> {
				if (neuronSoA.hasPendingFire(i)) {
					corticalNeuronLogic.delayedFire(now, i);
			}});
		
	    // 3. PROCESS SPIKE BUFFER (rimuove spike scaduti, avanza finestra temporale)
	    spikeBufferLogic.pollAndProcess(now);
	}
	
	/*
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
	*/
	public Abstract3DLayer getSensorsTargetLayer() {
		return getLayer(SENSORS_TARGET_LAYER);
	}

	public Abstract3DLayer getClassifiersSourceLayer() {
		return getLayer(CLASSIFIERS_TARGET_LAYER);
	}
/*
	public AbstractNeuron[] getAllNeurons() {
		return neurons;
	}
*/
	/*
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
	*/
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
		/*
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
	*/
		private void validate() {

		}

		public Hemisphere<L> build( MultiLayersConfig layersCfg ) throws Exception {
			validate();
			return emisphere.build(layersCfg);
		}
	}

	public NeuronBean[] getHemisphereNeurons() {
		return hemisphereNeurons;
	}

	public int getTotalNeurons() {
		return totalNeurons;
	}

	public void setTotalNeurons(int totalNeurons) {
		this.totalNeurons = totalNeurons;
	}

	public int getTotalSynapses() {
		return totalSynapses;
	}

	public int getHemisphereId() {
		return hemisphereId;
	}
}
