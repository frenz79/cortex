package com.cortex.globals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.brain.Brain;

public class GlobalContext {

    // Process timing: keep nanos internally
    private static final AtomicLong processCounter = new AtomicLong(0);
    private static final LongAdder processTimeNanos = new LongAdder();
		
	

	/**
     * Returns average process time in milliseconds since last call and resets counters.
     */
    public static long getAverageProcessTimeMillis() {
        long count = processCounter.getAndSet(0);
        if (count == 0) return 0L;
        long totalNanos = processTimeNanos.sumThenReset();
        // convert to milliseconds
        return TimeUnit.NANOSECONDS.toMillis(totalNanos / count);
    }
		
	private static final Queue<Synapse> recentlyActiveSynapses = new ConcurrentLinkedQueue<>();
	
	public static void addRecentlyActiveSynapses(Synapse synapse) {
        Objects.requireNonNull(synapse);
        recentlyActiveSynapses.add(synapse);
    }
	
	/**
     * Drain the queue and apply consumer to each. Consumer returns true to keep it in the queue,
     * false to drop it. Implementation drains into a temporary list to avoid holding locks.
     */
	public static void forEachActiveSynapse(Function<Synapse, Boolean> consumer) {
	    Objects.requireNonNull(consumer, "consumer");
	    List<Synapse> drained = new ArrayList<>(recentlyActiveSynapses.size());
	    Synapse s;
	    while ((s = recentlyActiveSynapses.poll()) != null) {
	        drained.add(s);
	    }
	    for (Synapse syn : drained) {
	        boolean keep = true;
	        try {
	            keep = Boolean.TRUE.equals(consumer.apply(syn));
	        } catch (RuntimeException ex) {
	            ex.printStackTrace();
	            // on exception, keep the synapse for retry
	            keep = true;
	        }
	        if (keep) {
	            recentlyActiveSynapses.add(syn);
	        }
	    }
	}	
	/*
	public static void traceNeuronFire(long time, AbstractNeuron neuron, int layerId) {
		if (layerId<0) return;
        layersStats.computeIfAbsent(layerId, 
        	k -> new MetricsRecorder(brain.getLayer(layerId))).neuronFired(neuron);
    }
	*/
	
	/*
    public static void traceSynapseWeightUpdated(long time, int layerId, float oldW, float newW) {
    	if (layerId<0) return;
        layersStats.computeIfAbsent(layerId, 
        	k -> new MetricsRecorder(brain.getLayer(layerId))).sumSynapticWeights(oldW, newW);
    }
     */  
	/*
	private static final ConcurrentHashMap<Integer, MetricsRecorder> layersStats = new ConcurrentHashMap<>();	

    public static LayerStats getAndResetStats(int layerId) {
        MetricsRecorder s = layersStats.get(layerId);
        return (s != null) ? s.getStatsAndReset() : null;
    }

    private static Brain brain;
    
	public static void setMultiSphericalLayer(Brain l) {
		brain = l;
	}
	*/
}
