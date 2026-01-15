package com.cortex.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Point3f;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.commons.Maths;
import com.cortex.commons.Pair;

import net.jafama.FastMath;

public class SphericalLayer extends Layer<SphericalLayerConfig> {
	
	public SphericalLayer(SphericalLayerConfig config) {
		super(config);
	}

	@Override
	public int connect( AbstractNeuron[] neurons, int minConn, int maxConn, float maxDistance, boolean incoming ) {
		int w = neurons.length;
		int connections = 0;
		for (int rx = 0; rx < w; rx++) {
			int connsCounter = random.nextInt(minConn, maxConn);
			List<Pair<AbstractNeuron,Float>> conn = new ArrayList<>(connsCounter);
			
			int startIdx = random.nextInt(getNeurons().length);
			
			for ( int i=startIdx+1; i<getNeurons().length; i++ ) {
				if ( evaluate(conn, connsCounter, getNeurons()[i], maxDistance, getNeurons()[startIdx], true)<0 ) {
					conn.add(new Pair<>(getNeurons()[startIdx], 0.0f));
					connections++; 
				} else {
					break;
				}
			}
			if ( conn.size()<connsCounter) {
				for ( int i=0; i<startIdx; i++ ) {
					if ( evaluate(conn, connsCounter, getNeurons()[i], maxDistance, getNeurons()[startIdx], true)<0 ) {
						conn.add(new Pair<>(getNeurons()[startIdx], 0.0f));
						connections++; 
					} else {
						break;
					}
				}
			}
		}
		return connections;
	}
	
	@Override
	public int connect( AbstractNeuron[][] neurons, int minConn, int maxConn, float maxDistance, boolean incoming ) {
		int w = neurons.length;
		int h = neurons[0].length;
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
    	    	
    	    	List<Pair<AbstractNeuron,Float>> nearestNeurons = 
    	    		pickFromNeighbourhood( minConn, maxConn, maxDistance, new Point3f(x,y,z), 0.0f);
    	    	
    	    	if ( incoming ) {
    	    		connections += Synapse.create( neurons[rx][ry], nearestNeurons, true );
    	    	} else {
    	    		connections += Synapse.create( nearestNeurons, neurons[rx][ry], true );
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
			Neuron p = Neuron.build(getId(), randomBoolean(config.getInhibitorFreq()) ,x,y,z);
			this.neurons[i] = p;
		}
		
		long endTime = System.nanoTime();
		System.out.println("L"+getId()+" generated "+getNeuronsCount()+" neurons in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
	}

	@Override
	public List<Pair<AbstractNeuron,Float>> pickFromNeighbourhood( int minConn, int maxConn, float maxDistance, Point3f src, float inhibProbability ){
		int connsCounter = random.nextInt(minConn, maxConn);
		List<Pair<AbstractNeuron,Float>> conn = new ArrayList<>(connsCounter);
		
		int startIdx = random.nextInt(getNeurons().length);
		boolean skipInhibitors = inhibProbability==0.0f; 
		
		for ( int i=startIdx; i<getNeurons().length; i++ ) {
			if ( evaluate(conn, connsCounter, getNeurons()[i], maxDistance, src, skipInhibitors)<0 ) {
				return conn;
			}
		}		
		for ( int i=0; i<startIdx; i++ ) {
			if ( evaluate(conn, connsCounter, getNeurons()[i], maxDistance, src, skipInhibitors)<0 ) {
				return conn;
			}
		}
		return conn;
	}
		
	@Override
	public List<Pair<AbstractNeuron,Float>> pickFromNeighbourhood( int minConn, int maxConn, float maxDistance, AbstractNeuron src, float inhibProbability ){
		int connsCounter = random.nextInt(minConn, maxConn);
		List<Pair<AbstractNeuron,Float>> conn = new ArrayList<>(connsCounter);
		
		int startIdx = random.nextInt(getNeurons().length);
		boolean skipInhibitors = inhibProbability==0.0f; 
		for ( int i=startIdx; i<getNeurons().length; i++ ) {
			if ( evaluate(conn, connsCounter, getNeurons()[i], maxDistance, src, skipInhibitors)<0 ) {
				return conn;
			}
		}		
		for ( int i=0; i<startIdx; i++ ) {
			if ( evaluate(conn, connsCounter, getNeurons()[i], maxDistance, src, skipInhibitors)<0 ) {
				return conn;
			}
		}
		return conn;
	}
	
	private int evaluate(List<Pair<AbstractNeuron,Float>> conn, int connsCounter, AbstractNeuron n, float maxDistance, AbstractNeuron src, boolean skipInhibitors){
		float distance = Maths.distance(n.getPosition(), src.getPosition());
		if ( src!=n && distance<maxDistance && (!skipInhibitors || n.isInhibitor()!=skipInhibitors)) {
			conn.add(new Pair<>(n,distance));
			if(conn.size()>=connsCounter) {
				return -1;
			}
		}
		return 1;
	}
	
	private int evaluate(List<Pair<AbstractNeuron,Float>> conn, int connsCounter, AbstractNeuron n, float maxDistance, Point3f src, boolean skipInhibitors){
		float distance = Maths.distance(n.getPosition(), src);
		if ( distance<maxDistance && (!skipInhibitors || n.isInhibitor()!=skipInhibitors)) {
			conn.add(new Pair<>(n,distance));
			if(conn.size()>=connsCounter) {
				return -1;
			}
		}
		return 1;
	}
}
