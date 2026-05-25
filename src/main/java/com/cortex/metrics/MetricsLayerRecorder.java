package com.cortex.metrics;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.brain.layers.Layer;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public class MetricsLayerRecorder {
	private final Layer layer;

	private final LongAdder spikeCounter = new LongAdder();
	private final Set<AbstractNeuron> activeNeurons = ConcurrentHashMap.newKeySet();
	private Set<AbstractNeuron> prevActive = ConcurrentHashMap.newKeySet();

	private final DoubleAdder absWeightSum = new DoubleAdder();
	
	public MetricsLayerRecorder(Layer layer) {
		this.layer = layer;
	}
	
	MetricsLayerRecorder neuronFired(AbstractNeuron neuron) {
		spikeCounter.increment();
		activeNeurons.add(neuron);
		return this;
	}

	MetricsLayerRecorder sumSynapticWeights(SynapseUpdatedData data) {
		absWeightSum.add(Math.abs(data.newValue() - data.oldValue()));
		return this;
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
				.excitatory();

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
				layer,
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

	public Layer getLayer() {
		return layer;
	}
}
