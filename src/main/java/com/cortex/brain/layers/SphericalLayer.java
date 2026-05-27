package com.cortex.brain.layers;

import java.util.Collection;
import java.util.Random;
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

public class SphericalLayer extends Layer {

	public SphericalLayer(LayerConfig config) {
		super(config);
	}

	@Override
	public int link( Layer layer, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg) {
		Random rnd = ThreadLocalRandom.current();
		int connections = 0;

		float cellSize = maxDistance; // scelta naturale

		for (AbstractNeuron src : layer.getNeurons()) {
			int k = rnd.nextInt(minConn, maxConn);
			IntList idxs = findKNearestApprox(
					src.getPosition().x(),
					src.getPosition().y(),
					src.getPosition().z(),
					k,
					cellSize,
					maxDistance
					);

			if (idxs.isEmpty()) continue;

			Collection<Neighbor> conns =
					toNeighbors(idxs, getNeurons(),
							src.getPosition().x(),
							src.getPosition().y(),
							src.getPosition().z());

			// applica filtro (inhibitory / excitatory)
			conns.removeIf(n -> !filter.test(n.neuron()));

			if (!conns.isEmpty()) {
				connections += Synapse.create(src, conns, synCfg);
			}
		}

		return connections;
	}

	@Override
	public int link(IClassifier<? extends AbstractNeuron> classifier, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg) {
		return link (classifier.getNeurons(), minConn, maxConn, maxDistance, filter, synCfg, true);
	}

	@Override
	public int link( ISensor sensor, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg ) {
		return link (sensor.getNeurons(), minConn, maxConn, maxDistance, filter, synCfg, false);
	}

	private final int link( AbstractNeuron[][] matrix, int minConn, int maxConn, float maxDistance, Predicate<AbstractNeuron> filter, SynapsePlasticityConfig synCfg, boolean isIncoming ) {
		int w = matrix.length;
		int h = matrix[0].length;
		int connections = 0;

	    float cellSize = maxDistance;
	    
		Random rnd = ThreadLocalRandom.current();
		for (int rx = 0; rx < w; rx++) {
			for (int ry = 0; ry < h; ry++) {
				float u = (rx + 0.5f) / w; // 0..1
				float v = (ry + 0.5f) / h; // 0..1

				// Sphere projection
				float theta = (float)(2 * Maths.PI * u);     // longitude
				float phi   = (float)(Maths.PI * (v - 0.5)); // latitude
				float cosPhi = (float)Maths.cos(phi);

				float x = (float)(cosPhi * Maths.cos(theta) * config.DIMENSION);
				float y = (float)(cosPhi * Maths.sin(theta) * config.DIMENSION);
				float z = (float)(Maths.sin(phi) * config.DIMENSION);

				IntList neighborsIdx = findKNearestApprox(x, y, z, rnd.nextInt(minConn, maxConn), cellSize, maxDistance);
				if (neighborsIdx.size() == 0) continue;
				Collection<Neighbor> conns = toNeighbors(neighborsIdx, getNeurons(), x, y, z);
				conns.removeIf(n -> !filter.test(n.neuron()));
				conns.removeIf(n -> n.getRealDistance() > maxDistance);
				if (!conns.isEmpty()) {
					if (!isIncoming)
						connections += Synapse.create( matrix[rx][ry], conns, synCfg );
					else
						connections += Synapse.create( conns, matrix[rx][ry], synCfg );
				}
			}
		}
		return connections;
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

		buildSpatialHash(config.DIMENSION / 2f);

		long endTime = System.nanoTime();
		System.out.println("L"+getLayerId()+" generated "+getNeuronsCount()+" neurons in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
		return this;
	}   
}
