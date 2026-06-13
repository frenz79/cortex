package com.cortex.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.Commons.SYNAPSE_SPEED;
import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.Functions;
import com.cortex.base.layers.LayerConnConfig;
import com.cortex.base.layers.Neighbor;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityRule;
import com.cortex.base.plasticity.IPlasticityRule;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityRule;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.CorticalNeuronsConfig;

public class SynapsesBuilder {

	final Logger logger = LogManager.getLogger(this.getClass());

	private final AbstractNeuron[] neurons;
	private final NeuronSynapses[] neuronSynapses;
	
	public void build() {
		for (int i=0; i<neurons.length; i++) {
			var n = neurons[i];
			NeuronSynapses ns = neuronSynapses[n.getIndex()];
			n.fillSynapseBranches( ns.toSynapseBranches() );
		}	
	}
	
	private final void add(NeuronSynapses[] syns, Synapse s, AbstractNeuron n, BranchType bt, boolean inc) {
		var ns = syns[n.getIndex()];
		if (ns==null) {
			ns = new NeuronSynapses();
			syns[n.getIndex()] = ns;
		}
		add(ns, s, n, bt, inc);
	}
	
	private final void add(NeuronSynapses ns, Synapse s, AbstractNeuron n, BranchType bt, boolean inc) {
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
		default:
			break;
		}
	}
	
	private final boolean areAlreadyConnected(NeuronSynapses[] syns, Synapse s, AbstractNeuron src, AbstractNeuron dst) {
		var srcSyns = syns[src.getIndex()];
		var dstSyns = syns[dst.getIndex()];
		if (srcSyns==null || dstSyns==null) return false;
		
		return srcSyns.contains(s) || dstSyns.contains(s);
	}
	private int getOutSynapsesCount(NeuronSynapses[] syns, AbstractNeuron n) {
		var ns = syns[n.getIndex()];
		return (ns==null)?0:ns.outFar.size()+ns.outNear.size();
	}

	private int getInSynapsesCount(NeuronSynapses[] syns, AbstractNeuron n) {
		var ns = syns[n.getIndex()];
		return (ns==null)?0:ns.inFar.size()+ns.inNear.size();
	}
	
	static class NeuronSynapses {
		public final Set<Synapse> inNear  = new HashSet<>();
		public final Set<Synapse> outNear = new HashSet<>();
		public final Set<Synapse> inFar   = new HashSet<>();
		public final Set<Synapse> outFar  = new HashSet<>();
		public final Set<Synapse> outFarFF= new HashSet<>();
		public final Set<Synapse> outFarFB= new HashSet<>();
		public final Set<Synapse> inFarFF = new HashSet<>();
		public final Set<Synapse> inFarFB = new HashSet<>();
		
		public boolean isAllEmpty() {
			return inNear.isEmpty()
				&&outNear.isEmpty()
				&&inFar.isEmpty()
				&&outFar.isEmpty()
				&&outFarFF.isEmpty()
				&&outFarFB.isEmpty()
				&&inFarFF.isEmpty()
				&&inFarFB.isEmpty();
		}
		public int size() {
			return inNear.size()
					+outNear.size()
					+inFar.size()
					+outFar.size()
					+outFarFF.size()
					+outFarFB.size()
					+inFarFF.size()
					+inFarFB.size();
		}
		public boolean hasInternalSynapses() {
			return !inNear.isEmpty()
				|| !outNear.isEmpty()
				|| !inFar.isEmpty()
				|| !outFar.isEmpty();
		}
		
		public boolean contains(Synapse s) {
			return inNear.contains(s)
					||outNear.contains(s)
					||inFar.contains(s)
					||outFar.contains(s)
					||outFarFF.contains(s)
					||outFarFB.contains(s)
					||inFarFF.contains(s)
					||inFarFB.contains(s);
		}
		
		public List<SynapseBranch> toSynapseBranches() {
			List<SynapseBranch> ret = new ArrayList<>(16);			
			ret.addAll(	splitIfBigger(inNear, true, BranchType.NEAR, 32) );
			ret.addAll(	splitIfBigger(inFar, true, BranchType.FAR, 32) );
			ret.addAll(	splitIfBigger(outNear, false, BranchType.NEAR, 32) );
			ret.addAll(	splitIfBigger(outFar, false, BranchType.FAR, 32) );
			ret.addAll(	splitIfBigger(outFarFF, false, BranchType.LAYER_FEEDFORWARD, 32) );
			ret.addAll(	splitIfBigger(outFarFB, false, BranchType.LAYER_FEEDBACK, 32) );
			ret.addAll(	splitIfBigger(inFarFF, true, BranchType.LAYER_FEEDFORWARD, 32) );
			ret.addAll(	splitIfBigger(inFarFB, true, BranchType.LAYER_FEEDBACK, 32) );
			return ret;
		}
		
		private Collection<SynapseBranch> splitIfBigger(Set<Synapse> syn, boolean inc, BranchType bt, int limit) {
		    List<SynapseBranch> ret = new ArrayList<>();

		    int size = syn.size();
		    if (size <= limit) {
		        ret.add(new SynapseBranch(syn.toArray(new Synapse[0]), inc, bt));
		        return ret;
		    }

		    Synapse[] arr = syn.toArray(new Synapse[0]);

		    for (int i = 0; i < size; i += limit) {
		        int end = Math.min(i + limit, size);
		        Synapse[] chunk = Arrays.copyOfRange(arr, i, end);
		        ret.add(new SynapseBranch(chunk, inc, bt));
		    }

		    return ret;
		}

		/*
		private Collection<? extends SynapseBranch> splitIfBigger(Set<Synapse> syn, boolean inc, BranchType bt, int limit) {
			List<SynapseBranch> ret = new ArrayList<>();			
			if (syn.size() > limit) {
				Synapse[] synArr = new Synapse[limit];
				int i=0;
				int count = 0;
				for ( Iterator<Synapse> iter=syn.iterator(); iter.hasNext(); ) {
					Synapse s = iter.next();
					synArr[i++] = s;					
					if (i==limit || i==syn.size()) {
						ret.add( new SynapseBranch(	synArr, inc, bt ));
						i=0;
						count++;
						synArr = new Synapse[syn.size()-limit*count];
					}
				}
			} else {
				ret.add( new SynapseBranch(	syn.toArray(new Synapse[0]), inc, bt ));
			}
			return ret;
		}
		*/
	}
	
	public SynapsesBuilder( AbstractNeuron[] neurons ) {
		this.neurons = neurons;
		this.neuronSynapses = new NeuronSynapses[neurons.length];
	}
	
	public void buildInternalSynapses( Abstract3DLayer layer ) {
		long startTime = System.nanoTime();
		AbstractNeuron[] ns = layer.getNeurons();		
		int N = ns.length;
		int connectionsCount = 0;
		int localCount = (int)(layer.getConfig().MAX_CONNECTIONS * 0.8f);
		int farCount   = layer.getConfig().MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();

		Map<AbstractNeuron, List<Neighbor>> neighbors = new HashMap<>((int)(N*1.2f));

		for (AbstractNeuron n : ns) {
			List<Neighbor> local = new ArrayList<>(
				Functions.findNearestNeurons(ns, n, localCount, layer.getConfig().CONNECTION_FILTER)
			);

			List<Neighbor> far = Functions.findRandomNeurons(ns, n, farCount);
			List<Neighbor> all = new ArrayList<>(local.size() + far.size());
			all.addAll(local);
			all.addAll(far);
			neighbors.put(n, all);
		}

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

				IPlasticityRule synPlast = src.isInhibitor() 
					?new InhibitorySynapticPlasticityRule( layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.inhibitory())
					:new ExcitatorySynapticPlasticityRule( layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatory());
						
				var s = Synapse.create(src, dst, target.getRealDistance(), baseSpeed, synPlast);
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
					
					IPlasticityRule synPlast = src.isInhibitor() 
						?new InhibitorySynapticPlasticityRule( layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.inhibitory())
						:new ExcitatorySynapticPlasticityRule( layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatory());
					
					Synapse s = Synapse.create(src, dst, target.getRealDistance(), baseSpeed, synPlast);
					
					if (!areAlreadyConnected(neuronSynapses, s, src, dst)) {
						add( neuronSynapses, s, src, BranchType.FAR, true  );
						add( neuronSynapses, s, dst, BranchType.FAR, false );
					}
					connectionsCount++;
					break;
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
					
					IPlasticityRule synPlast = src.isInhibitor() 
							?new InhibitorySynapticPlasticityRule( plasticityCfg.inhibitory())
							:new ExcitatorySynapticPlasticityRule( plasticityCfg.excitatory());
					
					Synapse s = Synapse.create(src, dst, target.getRealDistance(), baseSpeed, synPlast);
					
					if (!areAlreadyConnected(neuronSynapses, s, src, dst)) {
						add( neuronSynapses, s, src, bt, true  );
						add( neuronSynapses, s, dst, bt, false );
					}					
					connectionsCount++;
					break;
				}
			}
		}
		return connectionsCount;
	}

	public int buildExternalSynapses(Point3f pluginSite, AbstractNeuron[][] matrix, Abstract3DLayer targetLayer, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg, boolean isIncoming ) {
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
				x,y,z, rnd.nextInt(minConn, maxConn), 1.5f/*cellSize*/, 2.6f/*maxDistance*/,
				targetLayer.getNeurons(), targetLayer.getSpatialHash());

		List<Neighbor> conns = toNeighbors(neighborsIdx, targetLayer.getNeurons(), x,y,z);
		conns.removeIf(n -> !filter.test(n.neuron()));
		//conns.removeIf(n -> n.getRealDistance() > maxDistance);
		
		NeuronSynapses[] extNeuronSynapses = new NeuronSynapses[w*h];
		
		for (int rx = 0; rx < w; rx++) {
			for (int ry = 0; ry < h; ry++) {
				List<Integer> rndIdx = new ArrayList<>(60);
				for (int j=0; j<60; j++ ) {
					rndIdx.add( rnd.nextInt(0, conns.size()) );
				}

				AbstractNeuron extNeuron = matrix[rx][ry];
						
				for ( int i : rndIdx ) {
					IPlasticityRule synPlast = extNeuron.isInhibitor() 
							?new InhibitorySynapticPlasticityRule( synCfg.inhibitory())
							:new ExcitatorySynapticPlasticityRule( synCfg.excitatory());
					
					var intNeuron = conns.get(i);
					Synapse s;
					
					if (!isIncoming) {					
						s = Synapse.create( extNeuron, intNeuron.neuron(), intNeuron.getRealDistance(), baseSpeed, synPlast );
					} else {
						s = Synapse.create( intNeuron.neuron(), extNeuron, intNeuron.getRealDistance(), baseSpeed, synPlast );
					}
					
					var ns = extNeuronSynapses[to1DIndex(rx,ry,h)];
					if (!ns.contains(s)) {
						add(ns, s, extNeuron, BranchType.LAYER_FEEDFORWARD, isIncoming);
						add(ns, s, intNeuron.neuron(), BranchType.LAYER_FEEDFORWARD, !isIncoming);	
					}
					connections++;
				}
			}
		}
		return connections;
	}

	public static int to1DIndex(int row, int col, int numCols) {
        if (row < 0 || col < 0 || col >= numCols) {
            throw new IllegalArgumentException("Invalid row or column index");
        }
        return row * numCols + col;
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
}

	/*
	public static int create( AbstractNeuron srcNeuron, Neighbor toNeuron, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		return link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
	}

	public static int create( AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		return link( srcNeuron, toNeuron, distance, baseSpeed, near, plasticityCfg );
	}

	public static int create( Neighbor srcNeuron, AbstractNeuron toNeuron, long baseSpeed, boolean near, SynapsePlasticityConfig plasticityCfg ) {
		return link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
	}

	public static int create( AbstractNeuron srcNeuron, Collection<Neighbor> toNeurons, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor toNeuron : toNeurons) {
			link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
		}
		return toNeurons.size();
	}

	public static int create( Collection<Neighbor> srcNeurons, AbstractNeuron toNeuron, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor srcNeuron : srcNeurons) {
			link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
		}
		return srcNeurons.size();
	}

	private static final int link(AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, long baseSpeed, boolean near, SynapsePlasticityConfig plasticityCfg) {
		Synapse s = new Synapse( 
				srcNeuron, 
				toNeuron, 
				distance, 
				baseSpeed,
				srcNeuron.isInhibitor() 
				?new InhibitorySynapticPlasticityRule( plasticityCfg.inhibitory())
						:new ExcitatorySynapticPlasticityRule( plasticityCfg.excitatory())
				);
		if (srcNeuron.getLayerId()==toNeuron.getLayerId()) {
			srcNeuron.addSynapse( s, false, near); 
			toNeuron.addSynapse( s, true, near);
		} else {
			srcNeuron.addSynapse( s, false, srcNeuron.getLayerId()); 
			toNeuron.addSynapse( s, true, toNeuron.getLayerId());
		}
		return 1;
	}
	*/
/*
	// Used only at build time
	public void addSynapse(Synapse s, boolean incoming, boolean near) {
		try {
			int index = (incoming ? 0 : 2) + (near ? 0 : 1);
			if (synapsesBranchesTmp.length==1) {
				// Special case for sensors
				index = 0;
			}
			synchronized(synapsesBranchesTmp[index]) {
				synapsesBranchesTmp[index].add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add incoming:{} synapse to:{}",incoming, this.toString());
			throw ex;
		}
	}
	// Used only at build time
	public void addSynapse(Synapse s, boolean incoming, int layer) {
		try {
			int index = 4 + layer;
			if (synapsesBranchesTmp.length==1) {
				// Special case for sensors
				index = 0;
			} else {
				int maxLayers = (synapsesBranchesTmp.length-4)/2;
				if (!incoming) index += maxLayers;
			}
			synchronized(synapsesBranchesTmp[index]) {
				synapsesBranchesTmp[index].add(s);
			}
		} catch (Exception ex) {
			logger.error("Failed to add incoming:{} synapse to:{}",incoming, this.toString());
			throw ex;
		}
	}

	
	// Called only at build time
		public boolean hasOutSynapses() {
			if (synapsesBranchesTmp!=null) {
				for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
					if (!isIncomingBranch(i) && !synapsesBranchesTmp[i].isEmpty()){
						return true;
					}
				}
			} else {
				for (SynapseBranch sb : synapsesBranches) {
					if (!sb.incoming && sb.synapses.length>0) {
						return true;
					}
				}
			}
			return false;
		}
		// Called only at build time
		public boolean hasInSynapses() {
			if (synapsesBranchesTmp!=null) {
				for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
					if (isIncomingBranch(i) && !synapsesBranchesTmp[i].isEmpty()){
						return true;
					}
				}
			} else {
				for (SynapseBranch sb : synapsesBranches) {
					if (sb.incoming && sb.synapses.length>0) {
						return true;
					}
				}
			}
			return false;
		}
		// Called only at build time
		public int getInSynapsesCount() {
			int count = 0;
			if (synapsesBranchesTmp!=null) {
				for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
					if (isIncomingBranch(i)){
						count += synapsesBranchesTmp[i].size();
					}
				}
			} else {
				for (SynapseBranch sb : synapsesBranches) {
					if (sb.incoming) {
						count += sb.synapses.length;
					}
				}
			}
			return count;
		}
		// Called only at build time
		public int getOutSynapsesCount() {
			int count = 0;
			if (synapsesBranchesTmp!=null) {
				for (int i=0;i<this.synapsesBranchesTmp.length; i++) {
					if (!isIncomingBranch(i)){
						count += synapsesBranchesTmp[i].size();
					}
				}
			} else {
				for (SynapseBranch sb : synapsesBranches) {
					if (!sb.incoming) {
						count += sb.synapses.length;
					}
				}
			}
			return count;
		}
		*/
	
	/*
	 List<SynapseBranch> in = new ArrayList<>();
		List<SynapseBranch> out = new ArrayList<>();

		for (int i = 0; i < synapsesBranchesTmp.length; i++) {

			List<Synapse> list = synapsesBranchesTmp[i];
			Synapse[] arr = list.toArray(new Synapse[0]);

			boolean incoming = isIncomingBranch(i);

			if (arr.length > 32) { // TODO: from config

				// numero di sub-branches
				int numSplits = (int) Math.ceil(arr.length / 32.0);

				for (int s = 0; s < numSplits; s++) {

					int start = s * 32;
					int end = Math.min(start + 32, arr.length);

					Synapse[] split = Arrays.copyOfRange(arr, start, end);

					SynapseBranch b = new SynapseBranch(split, incoming);

					if (incoming) in.add(b);
					else out.add(b);
				}

			} else {

				SynapseBranch b = new SynapseBranch(arr, incoming);

				if (incoming) in.add(b);
				else out.add(b);
			}
		}

		incomingBranches = in.toArray(new SynapseBranch[0]);
		outgoingBranches = out.toArray(new SynapseBranch[0]);

		Arrays.fill(synapsesBranchesTmp, null);
		synapsesBranchesTmp = null;
		*/
	

