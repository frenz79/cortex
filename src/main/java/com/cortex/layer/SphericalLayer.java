package com.cortex.layer;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.brain.GlobalContext;
import com.cortex.commons.IntList;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;

import net.jafama.FastMath;

public class SphericalLayer extends Layer<SphericalLayerConfig> {

	public SphericalLayer(SphericalLayerConfig config) {
		super(config);
	}

	@Override
	public int link( Layer<?> layer, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter,	SynapsePlasticityConfig synCfg) {
		Random rnd = ThreadLocalRandom.current();
		int connections = 0;

		float cellSize = maxDistance; // scelta naturale

		for (Neuron src : layer.getNeurons()) {
			int k = rnd.nextInt(minConn, maxConn);
			IntList idxs = findKNearestApprox(
					src.getPosition().x,
					src.getPosition().y,
					src.getPosition().z,
					k,
					cellSize,
					maxDistance
					);

			if (idxs.isEmpty()) continue;

			Collection<Neighbor> conns =
					toNeighbors(idxs, getNeurons(),
							src.getPosition().x,
							src.getPosition().y,
							src.getPosition().z);

			// applica filtro (inhibitory / excitatory)
			conns.removeIf(n -> !filter.test(n.neuron()));

			if (!conns.isEmpty()) {
				connections += Synapse.create(src, conns, synCfg);
			}
		}

		return connections;
	}

	@Override
	public int link(IClassifier<? extends Neuron> classifier, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter, SynapsePlasticityConfig synCfg) {
		return link (classifier.getNeurons(), minConn, maxConn, maxDistance, filter, synCfg, true);
	}

	@Override
	public int link( ISensor sensor, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter, SynapsePlasticityConfig synCfg ) {
		return link (sensor.getNeurons(), minConn, maxConn, maxDistance, filter, synCfg, false);
	}

	private final int link( Neuron[][] matrix, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter, SynapsePlasticityConfig synCfg, boolean isIncoming ) {
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
				float theta = (float)(2 * FastMath.PI * u);     // longitude
				float phi   = (float)(FastMath.PI * (v - 0.5)); // latitude
				float cosPhi = (float)FastMath.cos(phi);

				float x = (float)(cosPhi * FastMath.cos(theta) * config.getRadius());
				float y = (float)(cosPhi * FastMath.sin(theta) * config.getRadius());
				float z = (float)(FastMath.sin(phi) * config.getRadius());

				IntList neighborsIdx = findKNearestApprox(x, y, z, rnd.nextInt(minConn, maxConn), cellSize, maxDistance);
				if (neighborsIdx.size() == 0) continue;
				Collection<Neighbor> conns = toNeighbors(neighborsIdx, getNeurons(), x, y, z);
				conns.removeIf(n -> !filter.test(n.neuron()));
				conns.removeIf(n -> n.distance() > maxDistance);
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
	public void generateNeurons() {
		long startTime = System.nanoTime();
		this.neurons = new Neuron[getNeuronsCount()];

		//  golden spiral / Fibonacci sphere variation
		float gr = (float) (3-Math.sqrt(5));
		float lambda = (float) (FastMath.PI * gr);
		final int counter = getNeuronsCount();
		final float radius = config.getRadius();

		for(int i=0; i<counter; i++){
			float t = (float)i/counter;
			float a1 = (float) FastMath.acos(1-2*t);
			float a2 = lambda * i;
			float sina1 = (float)FastMath.sin(a1)* radius;

			float x = sina1 * (float)FastMath.cos(a2);
			float y = sina1 * (float)FastMath.sin(a2);
			float z = (float) FastMath.cos(a1) * radius;
			Neuron p = GlobalContext.getNeuronFactory().buildNeuron(
					getId() 
					, config.isHasIncoming()
					, config.isHasOutgoing()
					, isInhibitor( )
					, x,y,z

					);
			this.neurons[i] = p;
		}

		buildSpatialHash(config.getRadius() / 2f);

		long endTime = System.nanoTime();
		System.out.println("L"+getId()+" generated "+getNeuronsCount()+" neurons in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
	}    
}
