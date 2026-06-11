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
import com.cortex.base.modules.IClassifier;
import com.cortex.base.modules.ISensor;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.Brain.CorticalNeuronFactory;
import com.cortex.brain.BrainLayersConnConfig.SYNAPSE_SPEED;

public abstract class AbstractLayer {

	final Logger logger = LogManager.getLogger(this.getClass());

	protected final LayerConfig config;
	protected AbstractNeuron[] neurons;
	private int synapsesCount = 0;

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
	
	public AbstractLayer connectInternal() {
		long startTime = System.nanoTime();
		AbstractNeuron[] ns = getNeurons();
		int N = ns.length;

		int localCount = (int)(config.MAX_CONNECTIONS * 0.8f);
		int farCount   = config.MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();
		
		Map<AbstractNeuron, List<Neighbor>> neighbors = new HashMap<>(N);

		for (AbstractNeuron n : ns) {
			List<Neighbor> local = new ArrayList<>(
				Functions.findNearestNeurons(ns, n, localCount, config.CONNECTION_FILTER)
			);

			List<Neighbor> far = Functions.findRandomNeurons(ns, n, farCount);
			List<Neighbor> all = new ArrayList<>(local.size() + far.size());
			all.addAll(local);
			all.addAll(far);
			neighbors.put(n, all);
		}

		int connectionsCount = 0;

		for (int round = 0; round < config.MAX_CONNECTIONS; round++) {
			for (AbstractNeuron src : ns) {		
				List<Neighbor> neigh = neighbors.get(src);
				if (neigh.isEmpty()) continue;

				Neighbor target = neigh.remove(neigh.size() - 1);
				AbstractNeuron dst = target.neuron();

				if (src == dst) continue;

				Point3f ps = src.getPosition();
				Point3f pd = dst.getPosition();

				float ds = ps.x()*ps.x() + ps.y()*ps.y() + ps.z()*ps.z();
				float dd = pd.x()*pd.x() + pd.y()*pd.y() + pd.z()*pd.z();

				if (ds >= dd) continue;

				Synapse.create(src, dst, target.getRealDistance(), baseSpeed, target.near(), config.SYNAPSE_PLASTICITY_CONFIG);
				connectionsCount++;
			}
		}

		// Check all neurons have at least one input / output
		for (AbstractNeuron src : ns) {
			if ( !src.hasOutSynapses() || !src.hasInSynapses() ) {
				List<Neighbor> far = Functions.findRandomNeurons(ns, src, farCount);
				while(!far.isEmpty()) {

					Neighbor target = far.remove(far.size() - 1);
					AbstractNeuron dst = target.neuron();

					if (src == dst) continue;

					Point3f ps = src.getPosition();
					Point3f pd = dst.getPosition();

					float ds = ps.x()*ps.x() + ps.y()*ps.y() + ps.z()*ps.z();
					float dd = pd.x()*pd.x() + pd.y()*pd.y() + pd.z()*pd.z();

					if (ds >= dd) continue;
					if (!src.hasOutSynapses()) {
						Synapse.create(src, dst, target.getRealDistance(), baseSpeed, target.near(), config.SYNAPSE_PLASTICITY_CONFIG);
					} else {
						Synapse.create(dst, src, target.getRealDistance(), baseSpeed, target.near(), config.SYNAPSE_PLASTICITY_CONFIG);
					}
					connectionsCount++;
					break;
				}
			}
		}

		long endTime = System.nanoTime();
		logger.info(
				"L{} generated {} synapses in {}",
				config.getLayerId(),
				connectionsCount,
				TimeUnit.NANOSECONDS.toMicros(endTime - startTime) + " micros"
				);

		this.synapsesCount += connectionsCount;
		return this;
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
