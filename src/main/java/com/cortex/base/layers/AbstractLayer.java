package com.cortex.base.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.layers.Emisphere.CorticalNeuronFactory;
import com.cortex.base.modules.IClassifier;
import com.cortex.base.modules.ISensor;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.BrainLayersConnConfig.SYNAPSE_SPEED;

public abstract class AbstractLayer {

	final Logger logger = LogManager.getLogger(this.getClass());

	protected final LayerConfig config;
	protected AbstractNeuron[] neurons;

	public AbstractLayer(LayerConfig config) {
		super();
		this.config = config;
	}

	public abstract AbstractLayer populate( CorticalNeuronFactory neuronFactory );

	// Used to connect current layer to another one
	public abstract int link( 
			AbstractLayer dstLayer,
			int minConn, 
			int maxConn, 
			float maxDistance, 
			long baseSpeed,
			Predicate<AbstractNeuron> filter, 
			SynapsePlasticityConfig synapsePlasticityConfig );

	// Used to connect a generic point to other ones
	public abstract int link(
			Point3f site, 
			AbstractNeuron[][] matrix, 
			int minConn, 
			int maxConn, 
			float maxDistance,
			Predicate<AbstractNeuron> filter, 
			SynapsePlasticityConfig synCfg, 
			boolean isIncoming );	
	
	public int link(IClassifier<? extends AbstractNeuron> classifier, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg) {
		return link (classifier.getPluginSite(), classifier.getNeurons(), minConn, maxConn, maxDistance, filter, synCfg, true);
	}

	public int link( ISensor sensor, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg ) {
		return link (sensor.getPluginSite(), sensor.getNeurons(), minConn, maxConn, maxDistance, filter, synCfg, false);
	}

	public static List<Neighbor> toNeighbors(IntList idxs, AbstractNeuron[] neurons, float px, float py, float pz) {
		ArrayList<Neighbor> out = new ArrayList<>(idxs.size());
		for (int i = 0; i < idxs.size(); i++) {
			int ni = idxs.get(i);
			float vx = neurons[ni].getPosition().x() - px;
			float vy = neurons[ni].getPosition().y() - py;
			float vz = neurons[ni].getPosition().z() - pz;
			float d = (float)Maths.sqrt(vx*vx + vy*vy + vz*vz);
			out.add(new Neighbor(neurons[ni], d, false));
		}
		return out;
	}

	public LayerConfig getConfig() {
		return config;
	}

	public int getLayerId() {
		return config.getLayerId();
	} 

	protected boolean randomBoolean( float trueProbability ) {
		return ThreadLocalRandom.current().nextFloat(0.0f, 1.0f)<=trueProbability;
	}

	protected boolean isInhibitor( ) {
		return randomBoolean( config.INHIBITOR_FREQ );
	}

	public AbstractNeuron[] getNeurons() {
		return neurons;
	}

	public int getNeuronsCount() {
		return config.NEURONS_COUNT;
	}

	public int getSynapsesCount() {
		return synapsesCount;
	}
}
