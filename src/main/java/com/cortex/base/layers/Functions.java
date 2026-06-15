package com.cortex.base.layers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.utils.IntFloatPair;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.base.utils.SpatialHash;

public class Functions {
	
	public static final IntList findKNearestApprox(Point3f p, int k, float cellSize, float maxDistance, AbstractNeuron[] neurons, SpatialHash spatialHash ) {
		return findKNearestApprox(p.x(), p.y(), p.z(), k, cellSize, maxDistance, neurons, spatialHash);
	}

	public static final IntList findKNearestApprox(float x, float y, float z, int k, float cellSize, float maxDistance, AbstractNeuron[] neurons, SpatialHash spatialHash ) {
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
					long key = getCellKey(cx + dx, cy + dy, cz + dz);
					IntList bucket = spatialHash.getBucket(key);
					if (bucket == null) continue;
					for (int bi = 0; bi < bucket.size(); bi++) {
						int ni = bucket.get(bi);
						float vx = neurons[ni].getPosition().x() - x;
						float vy = neurons[ni].getPosition().y() - y;
						float vz = neurons[ni].getPosition().z() - z;
						float d2 = vx*vx + vy*vy + vz*vz;
						if (d2 > maxDist2) continue; // applica maxDistance
						// inserimento ordinato in best (k piccolo => O(k) è ok)
						if (d2 < best[k-1].dist()) {
							int j = k-1;
							while (j > 0 && d2 < best[j-1].dist()) {
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
			if (best[i].idx() >= 0) result.add(best[i].idx());
		}
		return result;
	}
	
	public static final long getCellKey(int cx, int cy, int cz) {
		return (((long)cx) << 42) ^ (((long)cy) << 21) ^ (long)cz;
	}

	private static final int cellCoord(float v, float cellSize) {
		return Maths.floor(v / cellSize);
	}
	
	public static final List<Neighbor> findRandomNeurons(AbstractNeuron[] all, AbstractNeuron src, int count) {
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
	
	public static final Collection<Neighbor> findNearestNeurons( AbstractNeuron[] neurons, AbstractNeuron from, int N, Predicate<AbstractNeuron> filter ) {
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
	
}
