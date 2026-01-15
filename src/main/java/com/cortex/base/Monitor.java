package com.cortex.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.util.concurrent.AtomicDouble;

public class Monitor {

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
