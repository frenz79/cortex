package com.cortex.base.utils;

import java.util.HashMap;
import java.util.Map;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.layers.Functions;

public class SpatialHash {

	private final float cellSize;
	private final Map<Long, IntList> spatialHash;
	
	public SpatialHash(AbstractNeuron[] neurons, float cellSize) {
		this.cellSize = cellSize;
		this.spatialHash = new HashMap<>( (Maths.floor(neurons.length*1.5)) );
		int n = neurons.length;
		for (int i = 0; i < n; i++) {
			long key = getCellKey(neurons[i], cellSize);
			IntList list = spatialHash.get(key);
			if (list == null) {
				list = new IntList(32);
				spatialHash.put(key, list);
			}
			list.add(i);
		}
	}
	
	public IntList getSpatialHashCell( AbstractNeuron n ) {
		return this.spatialHash.get(getCellKey(n, cellSize));
	}

	public static final long getCellKey(int cx, int cy, int cz) {
		return (((long)cx) << 42) ^ (((long)cy) << 21) ^ (long)cz;
	}

	private static final int cellCoord(float v, float cellSize) {
		return Maths.floor(v / cellSize);
	}
	
	public static long getCellKey( AbstractNeuron n, float cellSize ) {
		int cx = cellCoord(n.getPosition().x(), cellSize);
		int cy = cellCoord(n.getPosition().y(), cellSize);
		int cz = cellCoord(n.getPosition().z(), cellSize);
		return getCellKey(cx, cy, cz);
	}

	public IntList getBucket(long key) {
		return spatialHash.get(key);
	}

	public float getCellSize() {
		return cellSize;
	}
}
