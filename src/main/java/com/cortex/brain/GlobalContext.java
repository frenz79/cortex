package com.cortex.brain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import javax.vecmath.Point3f;

import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.google.common.util.concurrent.AtomicDouble;

public class GlobalContext {

	private static NeuronEntry[] neurons;
	
	public static class NeuronEntry {
		public final Neuron neuron;
		public boolean active;

		public NeuronEntry(Neuron neuron) {
			super();
			this.neuron = neuron;
			this.active = false;
		}
	};
	
	public static class NeuronFactory {
		
		private static int counter = 0;
		
		NeuronFactory ( int nCount ) {
			neurons = new NeuronEntry[nCount];
		}
		
		public Neuron buildNeuron( int layerId, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, float x, float y, float z) {
			return buildNeuron(layerId, hasIncoming, hasOutgoing, inhibitor, new Point3f(x,y,z));
		}

		public synchronized Neuron buildNeuron( int layerId, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position) {
			Neuron n = new Neuron(counter, layerId, hasIncoming, hasOutgoing, inhibitor, position);
			neurons[counter++] = new NeuronEntry(n);
			return n;
		}		
	}
	
	private static int processCounter = 0;
	private static long processDeltaTimeMicros = 0;
	private static NeuronFactory neuronFactory;
	
	public static void initialize( int neuronsCount ) {
		neuronFactory = new NeuronFactory(neuronsCount);
	}
	
	public static NeuronFactory getNeuronFactory(  ) {
		return neuronFactory;
	}
	
	public static void streamActiveNeuron( Function<Neuron, Boolean> consumer ) {
		long startTime = System.nanoTime();
		for (int i=0; i<neurons.length; i++) {
			if (neurons[i].active) {
				consumer.apply(neurons[i].neuron);
			}
		}
		long endTime = System.nanoTime();
		processDeltaTimeMicros += TimeUnit.NANOSECONDS.toMillis(endTime-startTime);
		processCounter++;
	}

	public static long getAverageProcessTime() {
		if (processCounter>0) {
			long avgTime = processDeltaTimeMicros / processCounter;
			processCounter = 0;
			processDeltaTimeMicros = 0l;
			return avgTime;
		}
		return 0;
	}
	
	public static int getNeuronsCount() {
		return neurons.length;
	}
	
	public static void setActive(Neuron n) {
		neurons[n.getIndex()].active = true;
	}
	
	private static final List<Synapse> recentlyActiveSynapses = new ArrayList<>();
	private static final ReentrantLock synLock = new ReentrantLock(false);
	
	public static void addRecentlyActiveSynapses(Synapse synapse) {
		synLock.lock();
		try {
			recentlyActiveSynapses.add(synapse);
		} finally {
			synLock.unlock();
		}
	}
	
	public static void forEachActiveSynapse( Function<Synapse, Boolean> consumer ) {
		synLock.lock();
		try {
		    Iterator<Synapse> it = GlobalContext.recentlyActiveSynapses.iterator();
	
		    while (it.hasNext()) {
		        Synapse s = it.next();
		        if ( !consumer.apply(s)) {
		        	it.remove();
		        }
		    }
		} finally {
			synLock.unlock();
		}
	}	
	
	public static record LayerStats( 
		float averageSynapticWeight,
		int activeNeurons,
		int spikesCount
	) {	
	}
	public static class IntLayerStats {
		private final AtomicDouble weight = new AtomicDouble(0.0);
		private final AtomicInteger spikeCounter = new AtomicInteger(0);
		private final AtomicInteger synapsesWeightCounter = new AtomicInteger(0);
		private final Set<Neuron> activeNeurons = new HashSet<>();
		
	    public void neuronFired( Neuron neuron ) {
	        this.spikeCounter.incrementAndGet();
	        this.activeNeurons.add(neuron);
	    }
	    
	    public void sumSynapticWeights(float val) {
	    	this.weight.addAndGet(val);
	    	this.synapsesWeightCounter.incrementAndGet();
	    }
	    
	    public LayerStats getStatsAndReset() {
	    	LayerStats ret = new LayerStats(
	    		(float)synapsesWeightCounter.getAndSet(0) / (float)weight.getAndSet(0),
	    		activeNeurons.size(),
	    		spikeCounter.getAndSet(0)
	    	);
	    	
	    	activeNeurons.clear();
	    	return ret;
	    }
	}
	
	private static final Map<Integer,IntLayerStats> layersStats = new HashMap<>();
	
	public static void traceNeuronFire(long time, Neuron neuron, int layerId ) {
		layersStats.compute( layerId, (k, v) -> (v == null) ? new IntLayerStats() : v)
			.neuronFired( neuron );
	}

	public static void traceSynapseWeightUpdated(long time, int layerId, float oldW, float newW) {
		layersStats.compute( layerId, (k, v) -> (v == null) ? new IntLayerStats() : v)
		.sumSynapticWeights(newW-oldW);
	}
	
	public static LayerStats getAndResetStats( int layerId ) {
		IntLayerStats ret = layersStats.get( layerId );
		return (ret!=null)?ret.getStatsAndReset():null;
	}
	
}
