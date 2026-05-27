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

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.Brain.CorticalNeuronFactory;
import com.cortex.commons.IntList;
import com.cortex.commons.Maths;
import com.cortex.commons.Point3f;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;

public abstract class Layer {

	protected final LayerConfig config;
	protected AbstractNeuron[] neurons;
	private int synapsesCount = 0;
	private Map<Long, IntList> spatialHash;

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

	public static record Neighbor(AbstractNeuron neuron, float distance) { 

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
			Layer layer,
			int minConn, 
			int maxConn, 
			float maxDistance, 
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

	public Layer connectInternal( ) {
		long startTime = System.nanoTime();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		AbstractNeuron[] ns = getNeurons();
		Map<AbstractNeuron, List<Neighbor>> neighbors = new HashMap<>();

		for (AbstractNeuron n : ns) {
			int connsCounter = random.nextInt(config.MIN_CONNECTIONS, config.MAX_CONNECTIONS);
			neighbors.put(n, new ArrayList<>(findNearest(ns, n, connsCounter, config.CONNECTION_FILTER)));
		}
		
		int maxRounds = config.MAX_CONNECTIONS;
		int connectionsCount = 0;
		for (int round = 0; round < maxRounds; round++) {
		    for (AbstractNeuron src : ns) {
		        List<Neighbor> neigh = neighbors.get(src);
		        while (!neigh.isEmpty()) {
			        Neighbor target = neigh.remove(0);	
			        AbstractNeuron dst = target.neuron();
			        if (src != dst && !src.hasOutgoingTo(dst) && !dst.hasOutgoingTo(src)) {
			        	connectionsCount += Synapse.create(
			        		src, dst, target.getRealDistance(), config.SYNAPSE_PLASTICITY_CONFIG);
			        	break;
			        }
		        }
		    }
		}
		
		long endTime = System.nanoTime();
		System.out.println("L"+config.getLayerId()+" generated "+connectionsCount+" synapses in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
		this.synapsesCount += connectionsCount;
		return this;
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
					pq.add(new Neighbor(n, dist));
				} else if (dist < pq.peek().distance()) {
					pq.poll();
					pq.add(new Neighbor(n, dist));
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
		this.spatialHash = new HashMap<>( (Maths.floor(neurons.length*1.5)) );
		int n = neurons.length;
		for (int i = 0; i < n; i++) {
			int cx = cellCoord(neurons[i].getPosition().x(), cellSize);
			int cy = cellCoord(neurons[i].getPosition().y(), cellSize);
			int cz = cellCoord(neurons[i].getPosition().z(), cellSize);
			long key = cellKey(cx, cy, cz);
			IntList list = this.spatialHash.get(key);
			if (list == null) {
				list = new IntList(4);
				this.spatialHash.put(key, list);
			}
			list.add(i);
		}
		return;
	}

	private static long cellKey(int cx, int cy, int cz) {
		// pack three 21-bit signed ints into a long
		long a = (long)(cx & 0x1FFFFF);
		long b = (long)(cy & 0x1FFFFF);
		long c = (long)(cz & 0x1FFFFF);
		return (a << 42) | (b << 21) | c;
	}

	private static int cellCoord(float v, float cellSize) {
		return Maths.floor(v / cellSize);
	}

    private static record IntFloatPair(int idx, float dist) {/**/  }
    
    public IntList findKNearestApprox(Point3f p, int k, float cellSize, float maxDistance) {
    	return findKNearestApprox(p.x(), p.y(), p.z(), k, cellSize, maxDistance);
    }

	// TODO: avoid self connections!
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
    
    public static Collection<Neighbor> toNeighbors(IntList idxs, AbstractNeuron[] neurons, float px, float py, float pz) {
        ArrayList<Neighbor> out = new ArrayList<>(idxs.size());
        for (int i = 0; i < idxs.size(); i++) {
            int ni = idxs.get(i);
            float vx = neurons[ni].getPosition().x() - px;
            float vy = neurons[ni].getPosition().y() - py;
            float vz = neurons[ni].getPosition().z() - pz;
            float d = (float)Maths.sqrt(vx*vx + vy*vy + vz*vz);
            out.add(new Neighbor(neurons[ni], d));
        }
        return out;
    }

	public LayerConfig getConfig() {
		return config;
	}
}
