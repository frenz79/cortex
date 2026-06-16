package com.cortex.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.Commons.SYNAPSE_SPEED;
import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.externals.ExternalModule;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.Functions;
import com.cortex.base.layers.LayerConnConfig;
import com.cortex.base.layers.Neighbor;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.CorticalNeuronsConfig;

public final class SynapsesBuilder {

	final Logger logger = LogManager.getLogger(this.getClass());

	private final AbstractNeuron[] neurons;
	private final NeuronSynapses[] neuronSynapses;
	private final Map<ExternalModule,NeuronSynapses[]> extNeuronSynapses;
	
	public SynapsesBuilder( AbstractNeuron[] neurons ) {
		this.neurons = neurons;
		this.neuronSynapses = new NeuronSynapses[neurons.length];
		this.extNeuronSynapses = new HashMap<>();
	}
	
	public void build() {
		logger.info("Creating synapses branches..");
		Arrays.stream(neurons).parallel().forEach( n -> {
			NeuronSynapses ns = neuronSynapses[n.getIndex()];
			n.fillSynapseBranches( ns.toSynapseBranches() );
		});
		
		logger.info("Creating synapses branches for externals");
		
		extNeuronSynapses.entrySet().parallelStream().forEach( extEntry -> {
			for ( NeuronSynapses ns : extEntry.getValue() ) {
				List<SynapseBranch> sb = ns.toSynapseBranches();
				if (!ns.inExt.isEmpty()) {
					for ( Synapse s : ns.inExt ) {
						s.getTarget().fillSynapseBranches( sb );
						s.getSource().attachSynapseBranch( sb );
					}
				}
				if (!ns.outExt.isEmpty()) {
					for ( Synapse s : ns.outExt ) {
						s.getTarget().attachSynapseBranch( sb );
						s.getSource().fillSynapseBranches( sb );
					}
				}
			}
		});		
	}
	
	private static final void add(NeuronSynapses[] syns, Synapse s, AbstractNeuron n, BranchType bt, boolean inc) {
		var ns = syns[n.getIndex()];
		if (ns==null) {
			ns = new NeuronSynapses();
			syns[n.getIndex()] = ns;
		}
		add(ns, s, n, bt, inc);
	}
	
	private static final void add(NeuronSynapses ns, Synapse s, AbstractNeuron n, BranchType bt, boolean inc) {
		switch(bt) {
		case FAR:
			if (inc) ns.inFar.add(s);
			else ns.outFar.add(s);
			break;
		case LAYER_FEEDBACK:
			if (inc) ns.inFarFB.add(s);
			else ns.outFarFB.add(s);
			break;
		case LAYER_FEEDFORWARD:
			if (inc) ns.inFarFF.add(s);
			else ns.outFarFF.add(s);
			break;
		case NEAR:
			if (inc) ns.inNear.add(s);
			else ns.outNear.add(s);
			break;
		case EXTERNAL:
			if (inc) ns.inExt.add(s);
			else ns.outExt.add(s);
			break;
		}
	}
	
	private static final boolean areAlreadyConnected(NeuronSynapses[] syns, Synapse s, AbstractNeuron src, AbstractNeuron dst) {
		var srcSyns = syns[src.getIndex()];
		var dstSyns = syns[dst.getIndex()];
		if (srcSyns==null || dstSyns==null) return false;
		return srcSyns.contains(s) || dstSyns.contains(s);
	}
	
	private static int getOutSynapsesCount(NeuronSynapses[] syns, AbstractNeuron n) {
		var ns = syns[n.getIndex()];
		return (ns==null)?0:ns.outFar.size()+ns.outNear.size();
	}

	private static int getInSynapsesCount(NeuronSynapses[] syns, AbstractNeuron n) {
		var ns = syns[n.getIndex()];
		return (ns==null)?0:ns.inFar.size()+ns.inNear.size();
	}
	
	static final class NeuronSynapses {
		public final Set<Synapse> inNear  = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> outNear = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> inFar   = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> outFar  = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> outFarFF= ConcurrentHashMap.newKeySet();
		public final Set<Synapse> outFarFB= ConcurrentHashMap.newKeySet();
		public final Set<Synapse> inFarFF = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> inFarFB = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> inExt   = ConcurrentHashMap.newKeySet();
		public final Set<Synapse> outExt   = ConcurrentHashMap.newKeySet();
		
		public boolean isAllEmpty() {
			return inNear.isEmpty()
				&& outNear.isEmpty()
				&& inFar.isEmpty()
				&& outFar.isEmpty()
				&& outFarFF.isEmpty()
				&& outFarFB.isEmpty()
				&& inFarFF.isEmpty()
				&& inFarFB.isEmpty();
		}
		public int size() {
			return inNear.size()
				+ outNear.size()
				+ inFar.size()
				+ outFar.size()
				+ outFarFF.size()
				+ outFarFB.size()
				+ inFarFF.size()
				+ inFarFB.size();
		}
		public boolean hasInternalSynapses() {
			return !inNear.isEmpty()
				|| !outNear.isEmpty()
				|| !inFar.isEmpty()
				|| !outFar.isEmpty();
		}
		
		public boolean contains(Synapse s) {
			return inNear.contains(s)
				|| outNear.contains(s)
				|| inFar.contains(s)
				|| outFar.contains(s)
				|| outFarFF.contains(s)
				|| outFarFB.contains(s)
				|| inFarFF.contains(s)
				|| inFarFB.contains(s);
		}
		
		public List<SynapseBranch> toSynapseBranches() {
			List<SynapseBranch> ret = new ArrayList<>(16);	
			if (!inNear.isEmpty())
				ret.addAll(	splitIfBigger(inNear, true, BranchType.NEAR, 32) );
			if (!inFar.isEmpty())
				ret.addAll(	splitIfBigger(inFar, true, BranchType.FAR, 32) );
			if (!outNear.isEmpty())
				ret.addAll(	splitIfBigger(outNear, false, BranchType.NEAR, 32) );
			if (!outFar.isEmpty())
				ret.addAll(	splitIfBigger(outFar, false, BranchType.FAR, 32) );
			if (!outFarFF.isEmpty())
				ret.addAll(	splitIfBigger(outFarFF, false, BranchType.LAYER_FEEDFORWARD, 32) );
			if (!outFarFB.isEmpty())
				ret.addAll(	splitIfBigger(outFarFB, false, BranchType.LAYER_FEEDBACK, 32) );
			if (!inFarFF.isEmpty())
				ret.addAll(	splitIfBigger(inFarFF, true, BranchType.LAYER_FEEDFORWARD, 32) );
			if (!inFarFB.isEmpty())
				ret.addAll(	splitIfBigger(inFarFB, true, BranchType.LAYER_FEEDBACK, 32) );
			if (!inExt.isEmpty())
				ret.addAll(	Collections.singletonList(
		        	new SynapseBranch(inExt.toArray(new Synapse[0]), true, BranchType.EXTERNAL)) );
			if (!outExt.isEmpty())
				ret.addAll(	Collections.singletonList(
		        	new SynapseBranch(outExt.toArray(new Synapse[0]), false, BranchType.EXTERNAL)) );
			return ret;
		}
		
		private static Collection<SynapseBranch> splitIfBigger(Set<Synapse> syn, boolean inc, BranchType bt, int limit) {
		    int size = syn.size();
		    if (size <= limit) {
		        return Collections.singletonList(
		        	new SynapseBranch(syn.toArray(new Synapse[0]), inc, bt));
		    }
		    
		    List<SynapseBranch> ret = new ArrayList<>();
		    Synapse[] arr = syn.toArray(new Synapse[0]);

		    for (int i = 0; i < size; i += limit) {
		        int end = Math.min(i + limit, size);
		        Synapse[] chunk = Arrays.copyOfRange(arr, i, end);
		        ret.add(new SynapseBranch(chunk, inc, bt));
		    }
		    return ret;
		}
	}
	
	public void buildInternalSynapses( Abstract3DLayer layer ) {
		long startTime = System.nanoTime();
		AbstractNeuron[] ns = layer.getNeurons();		
		int N = ns.length;
		int connectionsCount = 0;
		int localCount = (int)(layer.getConfig().MAX_CONNECTIONS * 0.8f);
		int farCount   = layer.getConfig().MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();

		Map<AbstractNeuron, List<Neighbor>> neighbors = new ConcurrentHashMap<>((int)(N*1.2f));
		Arrays.stream(ns).parallel().forEach( n -> {
			List<Neighbor> local = new ArrayList<>(
				Functions.findNearestNeurons(ns, n, localCount, layer.getConfig().CONNECTION_FILTER)
			);

			List<Neighbor> far = Functions.findRandomNeurons(ns, n, farCount);
			List<Neighbor> all = new ArrayList<>(local.size() + far.size());
			all.addAll(local);
			all.addAll(far);
			neighbors.put(n, all);
		});

		for (int round = 0; round < layer.getConfig().MAX_CONNECTIONS; round++) {
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
						
				var s = Synapse.create(src, dst, target.getRealDistance(), baseSpeed, layer.getConfig().SYNAPSE_PLASTICITY_CONFIG);
				add( neuronSynapses, s, src, target.near()?BranchType.NEAR:BranchType.FAR, true  );
				add( neuronSynapses, s, dst, target.near()?BranchType.NEAR:BranchType.FAR, false );
				connectionsCount++;
			}
		}

		// Check all neurons have at least one input / output
		for (AbstractNeuron src : ns) {			
			if ( neuronSynapses[src.getIndex()]==null || neuronSynapses[src.getIndex()].hasInternalSynapses()) {
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
					Synapse s = Synapse.create(src, dst, target.getRealDistance(), baseSpeed, layer.getConfig().SYNAPSE_PLASTICITY_CONFIG);
					
					if (!areAlreadyConnected(neuronSynapses, s, src, dst)) {
						add( neuronSynapses, s, src, BranchType.FAR, true  );
						add( neuronSynapses, s, dst, BranchType.FAR, false );
						connectionsCount++;
						break;
					} else {
						Synapse.destroy(s);
					}
				}
			}
		}

		long endTime = System.nanoTime();
		logger.info(
			"L{} generated {} synapses in {}",
			layer.getLayerId(),
			connectionsCount,
			TimeUnit.NANOSECONDS.toMicros(endTime - startTime) + " micros"
		);
		return;
	}	

	// Generate Synapses between layers	
	public void buildLayersSynapses( List<LayerConnConfig> configs ) {	
		configs.parallelStream().forEach(
			e -> {
			Abstract3DLayer srcLayer = e.SOURCE_LAYER;
			Abstract3DLayer dstLayer = e.TARGET_LAYER;
			int connections = 0;
			
			connections += linkLayers(
				srcLayer,
				dstLayer, 
				e.MIN_CONNECTIONS, 
				e.MAX_CONNECTIONS,
				e.MAX_DISTANCE, 
				e.BASE_SPEED,
				e.NEURON_FILTER_PREDICATE,
				e.SYNAPSE_PLASTICITY_CONFIG
			);
			
			logger.info("L{} -> L{} : created {} synapses",
				srcLayer.getLayerId(),	
				dstLayer.getLayerId(),
				connections
			);
		});	
		logger.info("buildLayersSynapses..finished!");
	}

	private int linkLayers(
			Abstract3DLayer sourceLayer,
			Abstract3DLayer targetLayer,
			int minConn,
			int maxConn,
			float maxDistance,
			long baseSpeed,
			Predicate<AbstractNeuron> filter,
			SynapsePlasticityConfig plasticityCfg) {

		AbstractNeuron[] srcs = sourceLayer.getNeurons();
		AbstractNeuron[] dsts = targetLayer.getNeurons();
		
		CorticalNeuronsConfig neuronsConfig = targetLayer.getConfig().CORTICAL_NEURONS_CONFIG;

		int N = srcs.length;
		int connectionsCount = 0;

		// Precalcolo vicini per ogni sorgente
		Map<AbstractNeuron, List<Neighbor>> neighbors = new HashMap<>(N);
		for (AbstractNeuron src : srcs) {
			neighbors.put(src, new ArrayList<>(
				Functions.findNearestNeurons(dsts, src, maxConn, filter)
			));
		}

		BranchType bt = (sourceLayer.getLayerId()<targetLayer.getLayerId())
				?BranchType.LAYER_FEEDFORWARD
				:BranchType.LAYER_FEEDBACK;
		
		// Round-robin
		for (int round = 0; round < maxConn; round++) {
			for (AbstractNeuron src : srcs) {
				List<Neighbor> neigh = neighbors.get(src);
				if (neigh.isEmpty()) continue;

				int attempts = neigh.size();
				for (int i = 0; i < attempts; i++) {

					Neighbor target = neigh.remove(0);
					AbstractNeuron dst = target.neuron();

					if (src == dst) continue;

					Point3f ps = src.getPosition();
					Point3f pd = dst.getPosition();
					// Don't mind Z pos for inter-layers connections
					float ds = ps.x()*ps.x() + ps.y()*ps.y();// + ps.z()*ps.z();
					float dd = pd.x()*pd.x() + pd.y()*pd.y();// + pd.z()*pd.z();

					if (ds >= dd) continue;
					if ( getInSynapsesCount(neuronSynapses, dst)>=neuronsConfig.MAX_FAN_IN ) {
						continue;
					}
					if ( getOutSynapsesCount(neuronSynapses, src)>=neuronsConfig.MAX_FAN_OUT ) {
						continue;
					}
					
					Synapse s = Synapse.create(src, dst, target.getRealDistance(), baseSpeed, plasticityCfg);
					
					if (!areAlreadyConnected(neuronSynapses, s, src, dst)) {
						add( neuronSynapses, s, src, bt, true  );
						add( neuronSynapses, s, dst, bt, false );
						connectionsCount++;
						break;
					} else {
						Synapse.destroy(s);
					}
				}
			}
		}
		return connectionsCount;
	}

	public int buildExternalSynapses( ExternalModule ext, Abstract3DLayer targetLayer ) {
		return 	buildExternalSynapses(
			ext,
			ext.getPluginSite(),
			ext.getNeurons(),
			targetLayer,
			ext.getExternalConnConfig().CONNECTIONS,
			ext.getExternalConnConfig().MAX_DISTANCE,
			ext.getExternalConnConfig().NEURON_FILTER_PREDICATE,
			ext.getExternalConnConfig().SYNAPSE_PLASTICITY_CONFIG
		);
	}
	
	private int buildExternalSynapses(ExternalModule external, Point3f pluginSite, AbstractNeuron[][] matrix, Abstract3DLayer targetLayer, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg ) {
		int w = matrix.length;
		int h = matrix[0].length;
		int connections = 0;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();

		// Sphere projection
		float u = (pluginSite.x() + 0.5f) / w; // 0..1
		float v = (pluginSite.y() + 0.5f) / h; // 0..1

		float theta = (float)(2 * Maths.PI * u);     // longitude
		float phi   = (float)(Maths.PI * (v - 0.5)); // latitude
		float cosPhi = (float)Maths.cos(phi);

		float x = (float)(cosPhi * Maths.cos(theta) * targetLayer.getConfig().DIMENSION);
		float y = (float)(cosPhi * Maths.sin(theta) * targetLayer.getConfig().DIMENSION);
		float z = (float)(Maths.sin(phi) * targetLayer.getConfig().DIMENSION);

		Random rnd = ThreadLocalRandom.current();
		IntList neighborsIdx = Functions.findKNearestApprox(
				x,y,z, maxConn, 1.5f/*cellSize*/, 2.6f/*maxDistance*/,
				targetLayer.getNeurons(), targetLayer.getSpatialHash());

		List<Neighbor> conns = toNeighbors(neighborsIdx, targetLayer.getNeurons(), x,y,z);
		conns.removeIf(n -> !filter.test(n.neuron()));
		//conns.removeIf(n -> n.getRealDistance() > maxDistance);
		
		NeuronSynapses[] extNs = extNeuronSynapses.get(external);
		if (extNs==null) {
			extNs = new NeuronSynapses[w*h];
			extNeuronSynapses.put(external, extNs);
		}
				
		for (int rx = 0; rx < w; rx++) {
			for (int ry = 0; ry < h; ry++) {
				List<Integer> rndIdx = new ArrayList<>(maxConn);
				for (int j=0; j<maxConn; j++ ) {
					rndIdx.add( rnd.nextInt(0, conns.size()) );
				}

				AbstractNeuron extNeuron = matrix[rx][ry];
				
				for ( int i : rndIdx ) {
					var intNeuron = conns.get(i);
					Synapse s;
					
					if (external.isProducer()) {					
						s = Synapse.create( extNeuron, intNeuron.neuron(), intNeuron.getRealDistance(), baseSpeed, synCfg );
					} else {
						s = Synapse.create( intNeuron.neuron(), extNeuron, intNeuron.getRealDistance(), baseSpeed, synCfg );
					}
					
					int idx = to1DIndex(rx,ry,h);
					var ns = extNs[idx];
					if (ns==null || !ns.contains(s) /*!areAlreadyConnected(neuronSynapses, s, src, dst)*/) {
						if (ns==null) {
							ns = new NeuronSynapses();
							extNs[idx] = ns;
						}
						add(extNs, s, extNeuron, BranchType.EXTERNAL, !external.isProducer());
						add(neuronSynapses, s, intNeuron.neuron(), BranchType.EXTERNAL, external.isProducer());						
						connections++;
					} else {
						Synapse.destroy(s);
					}
					if (ns.size()>=maxConn) {
						break;
					}
				}
			}
		}
		return connections;
	}

	private static final int to1DIndex(int row, int col, int numCols) {
        if (row < 0 || col < 0 || col >= numCols) {
            throw new IllegalArgumentException("Invalid row or column index");
        }
        return row * numCols + col;
    }
	
	private static final List<Neighbor> toNeighbors(IntList idxs, AbstractNeuron[] neurons, float px, float py, float pz) {
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
}
