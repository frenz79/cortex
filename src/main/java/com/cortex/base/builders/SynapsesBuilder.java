package com.cortex.base.builders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.Commons.SYNAPSE_SPEED;
import com.cortex.base.beans.NeuronBean;
import com.cortex.base.beans.SynapseBean;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.layers.Functions;
import com.cortex.base.layers.LayerConnConfig;
import com.cortex.base.layers.Neighbor;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.soa.constants.BranchTypeCode;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.CorticalNeuronsConfig;

public final class SynapsesBuilder {

	final Logger logger = LogManager.getLogger(this.getClass());

//	private final SynapseSoA synapseStateBuff;
	private final NeuronBean[] neurons;
//	private final Map<ExternalModule,NeuronSynapses[]> extNeuronSynapses;
	
	public SynapsesBuilder( NeuronBean[] neurons ) {
		this.neurons = neurons;
//		this.extNeuronSynapses = new HashMap<>();
	}
	
	public void build() {
		/*
		logger.info("Creating synapses branches..");
		Arrays.stream(neurons).parallel().forEach( n -> {
			NeuronSynapses ns = neuronSynapses[n.getIndex()];
			List<SynapseBranch> sbList = ns.toSynapseBranches();
			for ( SynapseBranch sb : sbList ) {
				if (sb.direction == Direction.INCOMING)
				    n.addIncomingBranch(sb);
				else
				    n.addOutgoingBranch(sb);
			}
		});
		
		logger.info("Creating synapses branches for externals");
		
		extNeuronSynapses.entrySet().parallelStream().forEach( extEntry -> {
			for ( NeuronSynapses ns : extEntry.getValue() ) {
				List<SynapseBranch> sbList = ns.toSynapseBranches();
				for (SynapseBranch sb : sbList) {
				    if (sb.direction == Direction.INCOMING) {
				        // branch incoming → va al neurone target
				        sb.synapses[0].getTarget().addIncomingBranch(sb);
				    } else {
				        // branch outgoing → va al neurone source
				        sb.synapses[0].getSource().addOutgoingBranch(sb);
				    }
				}
			}
		});
		
		synapseStateBuff.allocate(Synapse.getSynapsesCount());
		// TODO: call init for each synapse
		/// 
		*/
	}
	
	public int buildInternalSynapses( Abstract3DLayer layer ) {
		long startTime = System.nanoTime();
		NeuronBean[] neurons = layer.getNeurons();		
		int len = layer.getNeuronsLen();
		int start = layer.getNeuronsStart();
				
		int connectionsCount = 0;
		int localCount = (int)(layer.getConfig().MAX_CONNECTIONS * 0.8f);
		int farCount   = layer.getConfig().MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();

		Map<NeuronBean, List<Neighbor>> neighbors = new ConcurrentHashMap<>((int)(len*1.2f));
		Arrays.stream(neurons,start,start+len).parallel().forEach( n -> {
			try {
				List<Neighbor> local = new ArrayList<>(
					Functions.findNearestNeurons(neurons, start, len, n, localCount, layer.getConfig().CONNECTION_FILTER)
				);
	
				List<Neighbor> far = Functions.findRandomNeurons(neurons, start, len, n, farCount);
				List<Neighbor> all = new ArrayList<>(local.size() + far.size());
				all.addAll(local);
				all.addAll(far);
				neighbors.put(n, all);
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		});

		for (int round = 0; round < layer.getConfig().MAX_CONNECTIONS; round++) {
			for (int i=0; i<len; i++) {
				NeuronBean src = neurons[i+start];
				List<Neighbor> neigh = neighbors.get(src);
				if (neigh.isEmpty()) continue;

				Neighbor target = neigh.remove(neigh.size() - 1);
				NeuronBean dst = target.neuron();

				if (src == dst) continue;

				Point3f ps = src.getPosition();
				Point3f pd = dst.getPosition();

				float ds = ps.x()*ps.x() + ps.y()*ps.y() + ps.z()*ps.z();
				float dd = pd.x()*pd.x() + pd.y()*pd.y() + pd.z()*pd.z();

				if (ds >= dd) continue;
						
				var s = new SynapseBean(
					src.id, 
					dst.id, 
					target.getRealDistance()
				);
				
				int branchType = target.near()?BranchTypeCode.NEAR:BranchTypeCode.FAR;
				src.addOutgoingSynapse(s, branchType);
				src.addIncomingSynapse(s, branchType);
				connectionsCount++;
			}
		}

		// Check all neurons have at least one input / output
		for (int i=0; i<len; i++) {
			NeuronBean src = neurons[i+start];		
			if ( !src.hasIncoming() || !src.hasOutgoing() ) {
				List<Neighbor> far = Functions.findRandomNeurons(neurons, start, len, src, farCount);
				while(!far.isEmpty()) {

					Neighbor target = far.remove(far.size() - 1);
					NeuronBean dst = target.neuron();

					if (src == dst) continue;

					Point3f ps = src.getPosition();
					Point3f pd = dst.getPosition();

					float ds = ps.x()*ps.x() + ps.y()*ps.y() + ps.z()*ps.z();
					float dd = pd.x()*pd.x() + pd.y()*pd.y() + pd.z()*pd.z();

					if (ds >= dd) continue;
					
					var s = new SynapseBean(
						src.id, 
						dst.id, 
						target.getRealDistance()
					);
					
					if (!src.isAlreadyConnectedTo(dst)) {
						src.addOutgoingSynapse(s, BranchTypeCode.FAR);
						src.addIncomingSynapse(s, BranchTypeCode.FAR);
						connectionsCount++;
						break;
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
		return connectionsCount;
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
			Predicate<NeuronBean> filter,
			SynapsePlasticityConfig plasticityCfg) {

		NeuronBean[] srcs = sourceLayer.getNeurons();
		NeuronBean[] dsts = targetLayer.getNeurons();
		
		CorticalNeuronsConfig neuronsConfig = targetLayer.getConfig().CORTICAL_NEURONS_CONFIG;

		int N = srcs.length;
		AtomicInteger connectionsCount = new AtomicInteger(0);

		// Precalcolo vicini per ogni sorgente
		Map<NeuronBean, List<Neighbor>> neighbors = new ConcurrentHashMap<>(N*2);
		Arrays.stream(srcs, sourceLayer.getNeuronsStart(), sourceLayer.getNeuronsStart()+sourceLayer.getNeuronsLen()).parallel().forEach( src -> {
			neighbors.put(src, new ArrayList<>(
				Functions.findNearestNeurons(dsts, targetLayer.getNeuronsStart(), targetLayer.getNeuronsLen(), src, maxConn, filter)
			));
		});

		int bt = (sourceLayer.getLayerId()<targetLayer.getLayerId())
				?BranchTypeCode.FEEDFORWARD
				:BranchTypeCode.FEEDBACK;
		
		// Round-robin
		for (int round = 0; round < maxConn; round++) {
			//for (AbstractNeuron src : srcs) {
			Arrays.stream(srcs, sourceLayer.getNeuronsStart(), sourceLayer.getNeuronsStart()+sourceLayer.getNeuronsLen()).parallel().forEach( src -> {
				List<Neighbor> neigh = neighbors.get(src);
				if (!neigh.isEmpty()) {

					int attempts = neigh.size();
					for (int i = 0; i < attempts; i++) {

						Neighbor target = neigh.remove(0);
						NeuronBean dst = target.neuron();

						if (src == dst) continue;

						Point3f ps = src.getPosition();
						Point3f pd = dst.getPosition();
						// Don't mind Z pos for inter-layers connections
						float ds = ps.x()*ps.x() + ps.y()*ps.y();// + ps.z()*ps.z();
						float dd = pd.x()*pd.x() + pd.y()*pd.y();// + pd.z()*pd.z();

						if (ds >= dd) continue;
						if ( dst.getIncomingSynapses()>=neuronsConfig.MAX_FAN_IN ) {
							continue;
						}
						if ( src.getOutgoingSynapses()>=neuronsConfig.MAX_FAN_OUT ) {
							continue;
						}
						
						var s = new SynapseBean(
								src.id, 
								dst.id, 
								target.getRealDistance()
							);
							
						if (!src.isAlreadyConnectedTo(dst)) {
							src.addOutgoingSynapse(s, BranchTypeCode.FAR);
							src.addIncomingSynapse(s, BranchTypeCode.FAR);
							connectionsCount.incrementAndGet();
							break;
						}							
					}
				}
			});
		}
		return connectionsCount.get();
	}
/*
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
	*/
	/*
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
				x,y,z, maxConn, maxDistance, maxDistance,
				targetLayer.getNeurons(), targetLayer.getSpatialHash());

		List<Neighbor> conns = toNeighbors(neighborsIdx, targetLayer.getNeurons(), x,y,z);
		conns.removeIf(n -> !filter.test(n.neuron()));
		conns.removeIf(n -> n.getRealDistance() > maxDistance);
		
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
						s = Synapse.create( synapseStateBuff, extNeuron, intNeuron.neuron(), intNeuron.getRealDistance(), baseSpeed, synCfg );
					} else {
						s = Synapse.create( synapseStateBuff, intNeuron.neuron(), extNeuron, intNeuron.getRealDistance(), baseSpeed, synCfg );
					}
					
					int idx = to1DIndex(rx,ry,h);
					var ns = extNs[idx];
					if (ns==null || (!ns.inExt.contains(s) && !ns.outExt.contains(s)) ) {
						if (ns==null) {
							ns = new NeuronSynapses();
							extNs[idx] = ns;
						}
						add(extNs, s, extNeuron, BranchType.EXTERNAL, !external.isProducer());
						add(neuronSynapses, s, intNeuron.neuron(), BranchType.EXTERNAL, external.isProducer());		
				//		logger.info("SENSOR Synapse:{} -> {}", extNeuron, intNeuron.neuron());
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
*/
	private static final int to1DIndex(int row, int col, int numCols) {
        if (row < 0 || col < 0 || col >= numCols) {
            throw new IllegalArgumentException("Invalid row or column index");
        }
        return row * numCols + col;
    }
	
	private static final List<Neighbor> toNeighbors(IntList idxs, NeuronBean[] neurons, float px, float py, float pz) {
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
	

/*
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
	
	private static int getOutSynapsesCount(NeuronSynapses[] syns, AbstractNeuron n) {
		var ns = syns[n.getIndex()];
		return (ns==null)?0:ns.outFar.size()+ns.outNear.size();
	}

	private static int getInSynapsesCount(NeuronSynapses[] syns, AbstractNeuron n) {
		var ns = syns[n.getIndex()];
		return (ns==null)?0:ns.inFar.size()+ns.inNear.size();
	}
	*/
	/*
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
				ret.addAll(	splitIfBigger(inNear, BranchType.NEAR, 32, Direction.INCOMING) );
			if (!inFar.isEmpty())
				ret.addAll(	splitIfBigger(inFar, BranchType.FAR, 32, Direction.INCOMING) );
			if (!outNear.isEmpty())
				ret.addAll(	splitIfBigger(outNear, BranchType.NEAR, 32, Direction.OUTGOING) );
			if (!outFar.isEmpty())
				ret.addAll(	splitIfBigger(outFar, BranchType.FAR, 32, Direction.OUTGOING) );
			if (!outFarFF.isEmpty())
				ret.addAll(	splitIfBigger(outFarFF, BranchType.LAYER_FEEDFORWARD, 32, Direction.OUTGOING) );
			if (!outFarFB.isEmpty())
				ret.addAll(	splitIfBigger(outFarFB, BranchType.LAYER_FEEDBACK, 32, Direction.OUTGOING) );
			if (!inFarFF.isEmpty())
				ret.addAll(	splitIfBigger(inFarFF, BranchType.LAYER_FEEDFORWARD, 32, Direction.INCOMING) );
			if (!inFarFB.isEmpty())
				ret.addAll(	splitIfBigger(inFarFB, BranchType.LAYER_FEEDBACK, 32, Direction.INCOMING) );
			if (!inExt.isEmpty())
				ret.addAll(	Collections.singletonList(
		        	new SynapseBranch(inExt.toArray(new Synapse[0]), BranchType.EXTERNAL, Direction.INCOMING)) );
			if (!outExt.isEmpty())
				ret.addAll(	Collections.singletonList(
		        	new SynapseBranch(outExt.toArray(new Synapse[0]), BranchType.EXTERNAL, Direction.OUTGOING)) );
			return ret;
		}
		
		private static Collection<SynapseBranch> splitIfBigger(Set<Synapse> syn, BranchType bt, int limit, Direction direction) {
		    int size = syn.size();
		    if (size <= limit) {
		        return Collections.singletonList(
		        	new SynapseBranch(syn.toArray(new Synapse[0]), bt, direction));
		    }
		    
		    List<SynapseBranch> ret = new ArrayList<>();
		    Synapse[] arr = syn.toArray(new Synapse[0]);

		    for (int i = 0; i < size; i += limit) {
		        int end = Math.min(i + limit, size);
		        Synapse[] chunk = Arrays.copyOfRange(arr, i, end);
		        ret.add(new SynapseBranch(chunk, bt, direction));
		    }
		    return ret;
		}
	}
	*/
}
