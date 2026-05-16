package com.cortex.layer;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.vecmath.Point3f;

import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.brain.GlobalContext;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;

import net.jafama.FastMath;

public class SphericalLayer extends Layer<SphericalLayerConfig> {
	
	public SphericalLayer(SphericalLayerConfig config) {
		super(config);
	}
	
	@Override
	public int link( Layer<?> layer, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter,	SynapsePlasticityConfig synapsePlasticityConfig) {
		int connections = 0;
		for (int i = 0; i < layer.getNeuronsCount(); i++) {
	    	Collection<Neighbor> conns = findNearest(
    	    		getNeurons(), 
    	    		layer.getNeurons()[i].getPosition(),
    	    		random.nextInt(minConn, maxConn), 
    	    		filter);
	    	connections += Synapse.create( layer.getNeurons()[i], conns, synapsePlasticityConfig );
		}
		return connections;
	}
	
	@Override
	public int link(IClassifier<? extends Neuron> classifier, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter, SynapsePlasticityConfig synapsePlasticityConfig) {
		Neuron[][] classifierMatrix = classifier.getNeurons();

		int w = classifierMatrix.length;
		int h = classifierMatrix[0].length;
		int connections = 0;
		
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
    	    	
    	    	Point3f pointOnSphere = new Point3f(x,y,z);
    	    	
    	    	Collection<Neighbor> conns = findNearest(
    	    		getNeurons(), 
    	    		pointOnSphere,
    	    		random.nextInt(minConn, maxConn), 
    	    		filter);
    	    	
    	    	connections += Synapse.create( conns, classifierMatrix[rx][ry], synapsePlasticityConfig );
    	    }
    	}
    	return connections;
	}
	
	@Override
	public int link( ISensor sensor, int minConn, int maxConn, float maxDistance, Predicate<Neuron> filter, SynapsePlasticityConfig synapsePlasticityConfig ) {
		Neuron[][] sensorMatrix = sensor.getNeurons();
		
		int w = sensorMatrix.length;
		int h = sensorMatrix[0].length;
		int connections = 0;
		
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
    	    	
    	    	Point3f pointOnSphere = new Point3f(x,y,z);
    	    	
    	    	Collection<Neighbor> conns = findNearest(
    	    		getNeurons(), 
    	    		pointOnSphere,
    	    		random.nextInt(minConn, maxConn), 
    	    		filter);
    	    	
    	    	connections += Synapse.create( sensorMatrix[rx][ry], conns, synapsePlasticityConfig );
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
		
		long endTime = System.nanoTime();
		System.out.println("L"+getId()+" generated "+getNeuronsCount()+" neurons in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
	}
}
