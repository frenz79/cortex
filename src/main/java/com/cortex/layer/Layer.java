package com.cortex.layer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.vecmath.Point3f;

import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;

import net.jafama.FastMath;

public abstract class Layer<C extends LayerConfig> {

	protected final Random random = new Random( System.nanoTime()+System.currentTimeMillis() );
	
	protected final int id;
	protected final C config;
	protected Neuron[] neurons;
	private int synapsesCount = 0;
	private static int seq = -1;
	
	public Layer(C config) {
		super();
		this.id = ++seq;
		this.config = config;
	}
	
	protected boolean randomBoolean( float trueProbability ) {
		return random.nextFloat(0.0f, 1.0f)<=trueProbability;
	}
	
	protected boolean isInhibitor( ) {
		return randomBoolean( config.getInhibitorFreq() );
	}
	
	public static record Neighbor(Neuron neuron, float distance) { 
		
		public float getRealDistance() {
			return (float)FastMath.sqrtQuick(distance);
		}		
	}
	
	public abstract void generateNeurons( );
	
	public abstract int link( 
		Layer<?> layer,
		int minConn, 
		int maxConn, 
		float maxDistance, 
		Predicate<Neuron> filter, 
		SynapsePlasticityConfig synapsePlasticityConfig );
	
	public abstract int link( 
		ISensor sensor,
		int minConn, 
		int maxConn, 
		float maxDistance, 
		Predicate<Neuron> filter, 
		SynapsePlasticityConfig synapsePlasticityConfig );
	
	public abstract int link( 
		IClassifier<? extends Neuron> classifier,
		int minConn, 
		int maxConn, 
		float maxDistance, 
		Predicate<Neuron> filter, 
		SynapsePlasticityConfig synapsePlasticityConfig );	
	
	
	public void connectInternal( ) {
		long startTime = System.nanoTime();
		Map<Neuron,Collection<Neighbor>> tmp = new ConcurrentHashMap<>();
		
		Arrays.stream(getNeurons()).parallel().forEach( n -> {	
			int connsCounter = random.nextInt(config.getMinConnections(), config.getMaxConnections());
			Collection<Neighbor> conns = findNearest(
				getNeurons(), 
				n.getPosition(), 
				connsCounter, 
				config.getConnectionFilter());		
			tmp.put(n, conns);
		});
		
		// Synapse creation made sync!
		int connectionsCount = 0;
		for ( Entry<Neuron, Collection<Neighbor>> e : tmp.entrySet() ) {
			Synapse.create(e.getKey(), e.getValue(), config.getSynapsePlasticityConfig());
			connectionsCount += e.getValue().size();
		}
				
		long endTime = System.nanoTime();
		System.out.println("L"+getId()+" generated "+connectionsCount+" synapses in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
		this.synapsesCount += connectionsCount;
	}

	public Collection<Neighbor> findNearest( Neuron[] neurons, Point3f target, int N, Predicate<Neuron> filter ) {
		/*
		PriorityBlockingQueue<Neighbor> pq = 
				new PriorityBlockingQueue<Neighbor>(N,(a,b) -> Float.compare(b.distance(), a.distance()));
		
		Arrays.stream(getNeurons()).parallel().forEach( n -> {	
			if (filter.test(n)) {			
				float dx = n.getPosition().x - target.x;
				float dy = n.getPosition().y - target.y;
				float dz = n.getPosition().z - target.z;
				float dist = dx*dx + dy*dy + dz*dz;
	
				if (pq.size() < N) {
					pq.add(new Neighbor(n, dist));
				} else if (dist < pq.peek().distance()) {
					pq.poll();
					pq.add(new Neighbor(n, dist));
				}
			}
		});
		*/
		PriorityQueue<Neighbor> pq =
			new PriorityQueue<>((a,b) -> Float.compare(b.distance(), a.distance()));

		for (int i = 0; i < neurons.length; i++) {
			Neuron n = neurons[i];
			if (filter.test(n)) {			
				float dx = n.getPosition().x - target.x;
				float dy = n.getPosition().y - target.y;
				float dz = n.getPosition().z - target.z;
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
	
	public Neuron[] getNeurons() {
		return neurons;
	}
	
	public int getNeuronsCount() {
		return config.getNeurons();
	}

	public int getId() {
		return id;
	}

	public int getSynapsesCount() {
		return synapsesCount;
	}
}
