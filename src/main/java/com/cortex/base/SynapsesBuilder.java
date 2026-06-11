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

import com.cortex.base.layers.AbstractLayer;
import com.cortex.base.layers.Functions;
import com.cortex.base.layers.Neighbor;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityRule;
import com.cortex.base.plasticity.IPlasticityRule;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityRule;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.BrainLayersConnConfig.SYNAPSE_SPEED;
import com.cortex.brain.CorticalNeuronsConfig;
import com.cortex.brain.LayerConnectionsConfig;

public class SynapsesBuilder {

	final Logger logger = LogManager.getLogger(this.getClass());
	
	private final List<Synapse> farSynapses = new ArrayList<>();
	private final List<Synapse> nearSynapses = new ArrayList<>();
	
	public void buildInternalSynapses( AbstractLayer layer ) {
		long startTime = System.nanoTime();
		AbstractNeuron[] ns = layer.getNeurons();		
		int N = ns.length;
		int connectionsCount = 0;
		int localCount = (int)(layer.getConfig().MAX_CONNECTIONS * 0.8f);
		int farCount   = layer.getConfig().MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();
		
		Map<AbstractNeuron, List<Neighbor>> neighbors = new HashMap<>(N);
		Set<AbstractNeuron> withIn = new HashSet<>();
		Set<AbstractNeuron> withOut = new HashSet<>();
		
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
						
				var s = new Synapse(src, dst, target.getRealDistance(), baseSpeed, synPlast);
				if (target.near()) {
					nearSynapses.add(s);
				} else {
					farSynapses.add(s);
				}
				
				withIn.add(dst);
				withOut.add(src);
				connectionsCount++;
			}
		}

		// Check all neurons have at least one input / output
		for (AbstractNeuron src : ns) {
			if ( !withOut.contains(src) || !withIn.contains(src) ) {
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
					Synapse s;
					
					IPlasticityRule synPlast = src.isInhibitor() 
						?new InhibitorySynapticPlasticityRule( layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.inhibitory())
						:new ExcitatorySynapticPlasticityRule( layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatory());
					
					if (!withOut.contains(src)) {
						s = new Synapse(src, dst, target.getRealDistance(), baseSpeed, synPlast);
					} else {
						s = new Synapse(dst, src, target.getRealDistance(), baseSpeed, synPlast);
					}
					farSynapses.add(s);
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
	public void buildExternalSynapses( List<LayerConnectionsConfig> configs ) {	
		configs.parallelStream().forEach(
			e -> {
			AbstractLayer srcLayer = e.SOURCE_LAYER;
			AbstractLayer dstLayer = e.TARGET_LAYER;
			int connections = 0;
			
			connections += link(
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

	public int link(
			AbstractLayer sourceLayer,
			AbstractLayer targetLayer,
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
					if ( dst.getInSynapsesCount()>=neuronsConfig.MAX_FAN_IN ) {
						continue;
					}
					if ( src.getOutSynapsesCount()>=neuronsConfig.MAX_FAN_OUT ) {
						continue;
					}
					Synapse.create(src, dst, target.getRealDistance(), baseSpeed, false, plasticityCfg);
					connectionsCount++;
					break;
				}
			}
		}

		return connectionsCount;
	}

	public int link(Point3f pluginSite, AbstractNeuron[][] matrix, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg, boolean isIncoming ) {
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

		float x = (float)(cosPhi * Maths.cos(theta) * config.DIMENSION);
		float y = (float)(cosPhi * Maths.sin(theta) * config.DIMENSION);
		float z = (float)(Maths.sin(phi) * config.DIMENSION);

		Random rnd = ThreadLocalRandom.current();
		IntList neighborsIdx = Functions.findKNearestApprox(
				x,y,z, rnd.nextInt(minConn, maxConn), 1.5f/*cellSize*/, 2.6f/*maxDistance*/,
				getNeurons(), spatialHash);

		List<Neighbor> conns = toNeighbors(neighborsIdx, getNeurons(), x,y,z);
		conns.removeIf(n -> !filter.test(n.neuron()));
		//conns.removeIf(n -> n.getRealDistance() > maxDistance);

		for (int rx = 0; rx < w; rx++) {
			for (int ry = 0; ry < h; ry++) {
				List<Integer> rndIdx = new ArrayList<>(60);
				for (int j=0; j<60; j++ ) {
					rndIdx.add( rnd.nextInt(0, conns.size()) );
				}

				for ( int i : rndIdx ) {
					if (!isIncoming)
						connections += Synapse.create( matrix[rx][ry], conns.get(i), baseSpeed, false, synCfg );
					else
						connections += Synapse.create( conns.get(i), matrix[rx][ry], baseSpeed, false, synCfg );
				}
			}
		}
		return connections;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

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


	public void compact() {

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
}
