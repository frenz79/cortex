package com.cortex.brain.layers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.Brain.CorticalNeuronFactory;
import com.cortex.brain.BrainLayersConnConfig.SYNAPSE_SPEED;
import com.cortex.commons.IntList;
import com.cortex.commons.Maths;
import com.cortex.commons.Point3f;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;

public abstract class Layer {

	final Logger logger = LogManager.getLogger(this.getClass());

	protected final LayerConfig config;
	protected AbstractNeuron[] neurons;
	private int synapsesCount = 0;
	private Map<Long, IntList> spatialHash;
	private float cellSize;

	public Layer(LayerConfig config) {
		super();
		this.config = config;
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

	public static record Neighbor(AbstractNeuron neuron, float distance, boolean near) { 

		public float getRealDistance() {
			return (float)Maths.sqrt(distance);
		}

		public AbstractNeuron neuron() {
			return neuron;
		}

		@Override
		public int hashCode() {
			return neuron.getIndex();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Neighbor other = (Neighbor) obj;
			return Objects.equals(neuron, other.neuron);
		}	
	}

	public abstract Layer populate( CorticalNeuronFactory neuronFactory );

	public abstract int link( 
			Layer dstLayer,
			int minConn, 
			int maxConn, 
			float maxDistance, 
			long baseSpeed,
			Predicate<AbstractNeuron> filter, 
			SynapsePlasticityConfig synapsePlasticityConfig );

	public abstract int link( 
			ISensor sensor,
			int minConn, 
			int maxConn, 
			float maxDistance, 
			Predicate<AbstractNeuron> filter, 
			SynapsePlasticityConfig synapsePlasticityConfig );

	public abstract int link( 
			IClassifier<? extends AbstractNeuron> classifier,
			int minConn, 
			int maxConn, 
			float maxDistance, 
			Predicate<AbstractNeuron> filter, 
			SynapsePlasticityConfig synapsePlasticityConfig );	

	public Layer connectInternal() {
		long startTime = System.nanoTime();
		AbstractNeuron[] ns = getNeurons();
		int N = ns.length;

		int localCount = (int)(config.MAX_CONNECTIONS * 0.8f);
		int farCount   = config.MAX_CONNECTIONS - localCount;
		long baseSpeed = SYNAPSE_SPEED.FAST.getBaseSpeed();
		
		Map<AbstractNeuron, List<Neighbor>> neighbors = new HashMap<>(N);

		for (AbstractNeuron n : ns) {
			List<Neighbor> local = new ArrayList<>(
				findNearest(ns, n, localCount, config.CONNECTION_FILTER)
			);

			List<Neighbor> far = pickRandomFarNeurons(ns, n, farCount);
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
				List<Neighbor> far = pickRandomFarNeurons(ns, src, farCount);
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

	private List<Neighbor> pickRandomFarNeurons(AbstractNeuron[] all, AbstractNeuron src, int count) {
		List<Neighbor> far = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			AbstractNeuron candidate;
			do {
				candidate = all[ThreadLocalRandom.current().nextInt(all.length)];
			} while (candidate == src);

			float dx = candidate.getPosition().x() - src.getPosition().x();
			float dy = candidate.getPosition().y() - src.getPosition().y();
			float dz = candidate.getPosition().z() - src.getPosition().z();
			float dist = dx*dx + dy*dy + dz*dz;

			far.add(new Neighbor(candidate, dist, false));
		}

		return far;
	}

	public Collection<Neighbor> findNearest( AbstractNeuron[] neurons, AbstractNeuron from, int N, Predicate<AbstractNeuron> filter ) {
		PriorityQueue<Neighbor> pq =
				new PriorityQueue<>((a,b) -> Float.compare(b.distance(), a.distance()));

		for (int i = 0; i < neurons.length; i++) {
			AbstractNeuron n = neurons[i];
			// Avoid self connections and loops
			if (n!=from && filter.test(n)) {			
				float dx = n.getPosition().x() - from.getPosition().x();
				float dy = n.getPosition().y() - from.getPosition().y();
				float dz = n.getPosition().z() - from.getPosition().z();
				float dist = dx*dx + dy*dy + dz*dz;

				if (pq.size() < N) {
					pq.add(new Neighbor(n, dist, true));
				} else if (dist < pq.peek().distance()) {
					pq.poll();
					pq.add(new Neighbor(n, dist, true));
				}
			}
		}		
		return pq;
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

	public void buildSpatialHash(float cellSize) {
		this.cellSize = cellSize;
		this.spatialHash = new HashMap<>( (Maths.floor(neurons.length*1.5)) );
		int n = neurons.length;
		for (int i = 0; i < n; i++) {
			long key = getCellKey(neurons[i]);
			IntList list = this.spatialHash.get(key);
			if (list == null) {
				list = new IntList(32);
				this.spatialHash.put(key, list);
			}
			list.add(i);
		}
		return;
	}

	public IntList getSpatialHashCell( AbstractNeuron n ) {
		return this.spatialHash.get(getCellKey(n));
	}

	private long getCellKey( AbstractNeuron n ) {
		int cx = cellCoord(n.getPosition().x(), cellSize);
		int cy = cellCoord(n.getPosition().y(), cellSize);
		int cz = cellCoord(n.getPosition().z(), cellSize);
		return cellKey(cx, cy, cz);
	}

	private static final long cellKey(int cx, int cy, int cz) {
		return (((long)cx) << 42) ^ (((long)cy) << 21) ^ (long)cz;
	}

	private static final int cellCoord(float v, float cellSize) {
		return Maths.floor(v / cellSize);
	}

	private static final record IntFloatPair(int idx, float dist) {/**/}

	public IntList findKNearestApprox(Point3f p, int k, float cellSize, float maxDistance) {
		return findKNearestApprox(p.x(), p.y(), p.z(), k, cellSize, maxDistance);
	}

	public IntList findKNearestApprox(float x, float y, float z, int k, float cellSize, float maxDistance) {
		// buffer ordinato di dimensione k (distanze quadratiche)
		IntFloatPair[] best = new IntFloatPair[k];
		for (int i = 0; i < k; i++) best[i] = new IntFloatPair(-1, Float.POSITIVE_INFINITY);

		int cx = cellCoord(x, cellSize);
		int cy = cellCoord(y, cellSize);
		int cz = cellCoord(z, cellSize);

		// radiusCells: 1 => 3x3x3; aumentare se necessario
		int radiusCells = 1;
		float maxDist2 = maxDistance * maxDistance;

		for (int dx = -radiusCells; dx <= radiusCells; dx++) {
			for (int dy = -radiusCells; dy <= radiusCells; dy++) {
				for (int dz = -radiusCells; dz <= radiusCells; dz++) {
					long key = cellKey(cx + dx, cy + dy, cz + dz);
					IntList bucket = spatialHash.get(key);
					if (bucket == null) continue;
					for (int bi = 0; bi < bucket.size(); bi++) {
						int ni = bucket.get(bi);
						float vx = getNeurons()[ni].getPosition().x() - x;
						float vy = getNeurons()[ni].getPosition().y() - y;
						float vz = getNeurons()[ni].getPosition().z() - z;
						float d2 = vx*vx + vy*vy + vz*vz;
						if (d2 > maxDist2) continue; // applica maxDistance
						// inserimento ordinato in best (k piccolo => O(k) è ok)
						if (d2 < best[k-1].dist) {
							int j = k-1;
							while (j > 0 && d2 < best[j-1].dist) {
								best[j] = best[j-1];
								j--;
							}
							best[j] = new IntFloatPair(ni, d2);
						}
					}
				}
			}
		}
		IntList result = new IntList(k);
		for (int i = 0; i < k; i++) {
			if (best[i].idx >= 0) result.add(best[i].idx);
		}
		return result;
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

	public Map<Long, IntList> getSpatialHash() {
		return spatialHash;
	}
}
