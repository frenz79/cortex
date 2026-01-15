package com.cortex.layer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Point3f;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.base.Synapse.PLASTICITY_RULE;
import com.cortex.commons.Pair;

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
	
	public abstract void generateNeurons();
	public abstract List<Pair<AbstractNeuron, Float>> pickFromNeighbourhood(int minConn, int maxConn, float maxDistance, AbstractNeuron src, float inhibProbability);
	public abstract List<Pair<AbstractNeuron, Float>> pickFromNeighbourhood(int minConn, int maxConn, float maxDistance, Point3f src, float inhibProbability);
	public abstract int connect(AbstractNeuron[][] neurons, int minConn, int maxConn, float maxDistance, boolean incoming);
	public abstract int connect(AbstractNeuron[]   neurons, int minConn, int maxConn, float maxDistance, boolean incoming);
	
	public void generateInternalConnections() {
		long startTime = System.nanoTime();
		
		Map<AbstractNeuron,List<Pair<AbstractNeuron, Float>>> tmp = new ConcurrentHashMap<>();
		
		Arrays.stream(getNeurons()).parallel().forEach( n -> {			
			List<Pair<AbstractNeuron, Float>> conn = pickFromNeighbourhood(
				config.getMinConnections(),
				config.getMaxConnections(),
				config.getMaxConnDistance(), 
				n,
				0.35f );// Intra-layer probability	
			tmp.put(n, conn);
		});
		
		// Synapse creation made sync!
		int connectionsCount = 0;
		for ( Entry<AbstractNeuron, List<Pair<AbstractNeuron, Float>>> e : tmp.entrySet() ) {
			Synapse.create(e.getKey(), e.getValue(), PLASTICITY_RULE.EXICITATORY);
			connectionsCount += e.getValue().size();
		}
				
		long endTime = System.nanoTime();
		System.out.println("L"+getId()+" generated "+connectionsCount+" synapses in "+TimeUnit.NANOSECONDS.toMicros(endTime-startTime)+" micros");
		this.synapsesCount += connectionsCount;
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
