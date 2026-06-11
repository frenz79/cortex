package com.cortex.base.layers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.Brain.CorticalNeuronFactory;
import com.cortex.brain.BrainLayersConnConfig.SYNAPSE_SPEED;
import com.cortex.brain.CorticalNeuronsConfig;

public final class SphericalLayer extends AbstractLayer {

	private Map<Long, IntList> spatialHash;
	private final float cellSize;
	
	public SphericalLayer(LayerConfig config) {
		super(config);
		this.cellSize = config.DIMENSION / 2.0f;
	}

	public IntList getSpatialHashCell( AbstractNeuron n ) {
		return this.spatialHash.get(Functions.getCellKey(n, cellSize));
	}

	@Override
	public SphericalLayer populate( CorticalNeuronFactory neuronFactory ) {
		long startTime = System.nanoTime();
		this.neurons = new AbstractNeuron[getNeuronsCount()];

		//  golden spiral / Fibonacci sphere variation
		float gr = (float) (3-Maths.sqrt(5));
		float lambda = (float) (Maths.PI * gr);
		final int counter = getNeuronsCount();
		final float radius = config.DIMENSION;

		for(int i=0; i<counter; i++){
			float t = (float)i/counter;
			float a1 = (float) Maths.acos(1-2*t);
			float a2 = lambda * i;
			float sina1 = (float)Maths.sin(a1)* radius;

			float x = sina1 * (float)Maths.cos(a2);
			float y = sina1 * (float)Maths.sin(a2);
			float z = (float) Maths.cos(a1) * radius;

			this.neurons[i] = neuronFactory.buildNeuron(
				config.getLayerId(),
				isInhibitor(), new Point3f(x,y,z)
			);
		}

		this.spatialHash = Functions.buildSpatialHash(getNeurons(), cellSize );

		long endTime = System.nanoTime();
		logger.info("L{} generated {} neurons in {} micros",
			getLayerId(),
			getNeuronsCount(),
			TimeUnit.NANOSECONDS.toMicros(endTime-startTime)
		);
		return this;
	} 

	@Override
	public int link(
			AbstractLayer targetLayer,
			int minConn,
			int maxConn,
			float maxDistance,
			long baseSpeed,
			Predicate<AbstractNeuron> filter,
			SynapsePlasticityConfig plasticityCfg) {

		AbstractNeuron[] srcs = this.getNeurons();
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

	@Override
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
}
