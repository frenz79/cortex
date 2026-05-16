package com.cortex.brain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import javax.vecmath.Point3f;

import com.cortex.base.Neuron;
import com.cortex.base.Synapse;

public class GlobalContext {

	private static volatile NeuronEntry[] neurons;
	
	public static class NeuronEntry {
		public final Neuron neuron;
		public volatile boolean active;

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
	
    // Process timing: keep nanos internally
    private static final AtomicLong processCounter = new AtomicLong(0);
    private static final LongAdder processTimeNanos = new LongAdder();
	private static volatile NeuronFactory neuronFactory;
	
	public static void initialize( int neuronsCount ) {
		neuronFactory = new NeuronFactory(neuronsCount);
	}
	
	public static NeuronFactory getNeuronFactory(  ) {
		return neuronFactory;
	}
	
	/**
     * Iterate active neurons and call consumer. Consumer returns true to keep active flag true,
     * false to clear it. This method is safe to call concurrently.
     */
    public static void streamActiveNeuron(Function<Neuron, Boolean> consumer) {
        long start = System.nanoTime();
        NeuronEntry[] snapshot = neurons; // volatile read
        if (snapshot == null) return;
        for (int i = 0; i < snapshot.length; i++) {
            NeuronEntry entry = snapshot[i];
            if (entry == null) continue;
            if (entry.active) {
                Boolean stay = Boolean.TRUE;
                try {
                    stay = consumer.apply(entry.neuron);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    // on exception, keep neuron active to be retried later
                    stay = Boolean.TRUE;
                }
                entry.active = Boolean.TRUE.equals(stay);
            }
        }
        long end = System.nanoTime();
        processTimeNanos.add(end - start);
        processCounter.incrementAndGet();
    }

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
	
	public static int getNeuronsCount() {
		return neurons.length;
	}
	
	public static void setActive(Neuron n) {
		neurons[n.getIndex()].active = true;
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
	
	public static record LayerStats( 
		float averageSynapticWeight,
		int activeNeurons,
		int spikesCount
	) {	
	}
	public static class IntLayerStats {
	    private final DoubleAdder weightSum = new DoubleAdder(); // sum of weight deltas
        private final LongAdder synapseWeightCount = new LongAdder(); // number of weight contributions
        private final LongAdder spikeCounter = new LongAdder();
        private final Set<Neuron> activeNeurons = ConcurrentHashMap.newKeySet();
		
        public void neuronFired(Neuron neuron) {
            spikeCounter.increment();
            activeNeurons.add(neuron);
        }

        public void sumSynapticWeights(double val) {
            weightSum.add(val);
            synapseWeightCount.increment();
        }

        public LayerStats getStatsAndReset() {
            long spikes = spikeCounter.sumThenReset();
            long count = synapseWeightCount.sumThenReset();
            double sum = weightSum.sumThenReset();
            int active = activeNeurons.size();
            activeNeurons.clear();
            float avgWeight = (count == 0) ? 0.0f : (float) (sum / (double) count);
            return new LayerStats(avgWeight, active, (int) spikes);
        }
	}
	
	private static final ConcurrentHashMap<Integer, IntLayerStats> layersStats = new ConcurrentHashMap<>();	
	
	public static void traceNeuronFire(long time, Neuron neuron, int layerId) {
        layersStats.computeIfAbsent(layerId, k -> new IntLayerStats()).neuronFired(neuron);
    }

    public static void traceSynapseWeightUpdated(long time, int layerId, float oldW, float newW) {
        double delta = (double) newW - (double) oldW;
        layersStats.computeIfAbsent(layerId, k -> new IntLayerStats()).sumSynapticWeights(delta);
    }

    public static LayerStats getAndResetStats(int layerId) {
        IntLayerStats s = layersStats.get(layerId);
        return (s != null) ? s.getStatsAndReset() : null;
    }
	
}
