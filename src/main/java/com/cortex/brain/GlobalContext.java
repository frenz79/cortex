package com.cortex.brain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

import com.cortex.base.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.layer.MultiSphericalLayer;
import com.cortex.layer.SphericalLayer;

public class GlobalContext {

	 private static final AtomicLong GLOBAL_TIME = new AtomicLong();

	    public static long now() {
	        return GLOBAL_TIME.get();
	    }

	    public static void tick(long t) {
	        GLOBAL_TIME.set(t);
	    }
	    
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
    
    public static void streamAllNeurons(Function<Neuron, Boolean> consumer) {
    	long start = System.nanoTime();
        NeuronEntry[] snapshot = neurons; // volatile read
        if (snapshot == null) return;
        for (int i = 0; i < snapshot.length; i++) {
            NeuronEntry entry = snapshot[i];
            consumer.apply(entry.neuron);
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
	
	public static record LayerStatsRec( 
		    double averageSynapticWeight,
		    
		    // if 0     -> collapsing
		    // if W_MAX -> saturating
            // oscillating -> layer is learning
		    double synapticWeightStdDev,
		    
		    long activeNeurons,
		    long totalNeurons,
		    long spikesCount,
		    
		    // How fast are active neurons
		    double avgFiringRateActive,
		    // How much the layer is alive
		    double avgFiringRateAll,
		    
		    // if too dense -> noise
		    // if too sparse -> dead
		    double sparsity,
		    double saturatedMinRatio,
		    
		    // Should not be > 1 : % synapses that reached W_MAX
		    double saturatedMaxRatio,
		    
		    // Actives tournover
		    // if fixed -> attractor
		    // 
		    double activationStability,
		    
		    double totalPlasticity,
		    
		    double energy
	) {	
	}
	public static class LayerStats {

	    private final SphericalLayer layer;

	    private final LongAdder spikeCounter = new LongAdder();
	    private final Set<Neuron> activeNeurons = ConcurrentHashMap.newKeySet();
	    private Set<Neuron> prevActive = ConcurrentHashMap.newKeySet();

	    private final DoubleAdder absWeightSum = new DoubleAdder();

	    public LayerStats(SphericalLayer layer) {
	        this.layer = layer;
	    }

	    public void neuronFired(Neuron neuron) {
	        spikeCounter.increment();
	        activeNeurons.add(neuron);
	    }

	    public void sumSynapticWeights(double oldW, double newW) {
	        absWeightSum.add(Math.abs(newW - oldW));
	    }

	    public LayerStatsRec getStatsAndReset() {

	        long spikesCount = spikeCounter.sumThenReset();
	        int activeCount = activeNeurons.size();
	        int totalNeurons = layer.getNeuronsCount();

	        double avgFiringRateActive = activeCount == 0 ? 0.0 :
	                (double) spikesCount / activeCount;

	        double avgFiringRateAll = (double) spikesCount / totalNeurons;
	        double sparsity = 1.0 - ((double) activeCount / totalNeurons);

	        // ---------------------------------------------------------
	        //  SCANSIONE COMPLETA DEI PESI REALI
	        // ---------------------------------------------------------
	        double sum = 0.0;
	        double sum2 = 0.0;
	        long count = 0;

	        long saturatedMin = 0;
	        long saturatedMax = 0;

	        ExcitatorySynapticPlasticityConfig cfg = layer.getConfig()
	                       .getSynapsePlasticityConfig()
	                       .excitatorySynapticPlasticityConfig();
	        
	        for (Neuron n : layer.getNeurons()) {
	        	for ( Synapse s : n.getInSynapses() ) {
		            float w = s.getWeight();
		            sum += w;
		            sum2 += w * w;
		            count++;
	
		            if (w <= cfg.W_MIN + 1e-6) saturatedMin++;
		            if (w >= cfg.W_MAX - 1e-6) saturatedMax++;
	        	}
	        }

	        double averageSynapticWeight = count == 0 ? 0.0 : sum / count;
	        double variance = count == 0 ? 0.0 : (sum2 / count) - (averageSynapticWeight * averageSynapticWeight);
	        double synapticWeightStdDev = Math.sqrt(Math.max(variance, 0.0));

	        double saturatedMinRatio = count == 0 ? 0.0 : (double) saturatedMin / count;
	        double saturatedMaxRatio = count == 0 ? 0.0 : (double) saturatedMax / count;

	        double totalPlasticity = absWeightSum.sumThenReset();
	        double energy = spikesCount * Math.abs(averageSynapticWeight + synapticWeightStdDev);

	        // ---------------------------------------------------------
	        //  STABILITÀ ATTIVAZIONE
	        // ---------------------------------------------------------
	        int overlap = 0;
	        for (Neuron n : activeNeurons) {
	            if (prevActive.contains(n)) overlap++;
	        }

	        double activationStability = activeCount == 0 ? 0.0 :
	                (double) overlap / activeCount;

	        prevActive = new HashSet<>(activeNeurons);
	        activeNeurons.clear();

	        return new LayerStatsRec(
	                averageSynapticWeight,
	                synapticWeightStdDev,
	                activeCount,
	                totalNeurons,
	                spikesCount,
	                avgFiringRateActive,
	                avgFiringRateAll,
	                sparsity,
	                saturatedMinRatio,
	                saturatedMaxRatio,
	                activationStability,
	                totalPlasticity,
	                energy
	        );
	    }
	}
	
	public static void traceNeuronFire(long time, Neuron neuron, int layerId) {
		if (layerId<0) return;
        layersStats.computeIfAbsent(layerId, k -> new LayerStats(layer.getLayers(layerId))).neuronFired(neuron);
    }

    public static void traceSynapseWeightUpdated(long time, int layerId, float oldW, float newW) {
    	if (layerId<0) return;
        layersStats.computeIfAbsent(layerId, k -> new LayerStats(layer.getLayers(layerId))).sumSynapticWeights(oldW, newW);
    }
    
    
	private static final ConcurrentHashMap<Integer, LayerStats> layersStats = new ConcurrentHashMap<>();	

    public static LayerStatsRec getAndResetStats(int layerId) {
        LayerStats s = layersStats.get(layerId);
        return (s != null) ? s.getStatsAndReset() : null;
    }

    private static MultiSphericalLayer layer;
    
	public static void setMultiSphericalLayer(MultiSphericalLayer l) {
		layer = l;
	}
	
}
