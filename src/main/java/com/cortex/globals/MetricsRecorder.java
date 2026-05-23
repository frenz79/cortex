package com.cortex.globals;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.brain.layers.SphericalLayer;

public class MetricsRecorder {

    private final SphericalLayer layer;

    private final LongAdder spikeCounter = new LongAdder();
    private final Set<AbstractNeuron> activeNeurons = ConcurrentHashMap.newKeySet();
    private Set<AbstractNeuron> prevActive = ConcurrentHashMap.newKeySet();

    private final DoubleAdder absWeightSum = new DoubleAdder();

    public MetricsRecorder(SphericalLayer layer) {
        this.layer = layer;
    }

    public void neuronFired(AbstractNeuron neuron) {
        spikeCounter.increment();
        activeNeurons.add(neuron);
    }

    public void sumSynapticWeights(double oldW, double newW) {
        absWeightSum.add(Math.abs(newW - oldW));
    }
    
    public static record LayerStats( 
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

    public LayerStats getStatsAndReset() {

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
                       .SYNAPSE_PLASTICITY_CONFIG
                       .excitatorySynapticPlasticityConfig();
        
        for (AbstractNeuron n : layer.getNeurons()) {
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
        for (AbstractNeuron n : activeNeurons) {
            if (prevActive.contains(n)) overlap++;
        }

        double activationStability = activeCount == 0 ? 0.0 :
                (double) overlap / activeCount;

        prevActive = new HashSet<>(activeNeurons);
        activeNeurons.clear();

        return new LayerStats(
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