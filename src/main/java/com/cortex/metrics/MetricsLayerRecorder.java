package com.cortex.metrics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.brain.layers.Layer;
import com.cortex.commons.Maths;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public class MetricsLayerRecorder {
	protected final Logger logger = LogManager.getLogger(this.getClass());
	
	private final Layer layer;

	private final LongAdder spikeCounter = new LongAdder();
	private final Set<AbstractNeuron> activeNeurons = ConcurrentHashMap.newKeySet();
	private Set<AbstractNeuron> prevActive = ConcurrentHashMap.newKeySet();
	
	private final DoubleAdder absWeightSum = new DoubleAdder();
	private final DoubleAdder sumWeights = new DoubleAdder();
	private final DoubleAdder sumWeights2 = new DoubleAdder();
	private final LongAdder saturatedMin = new LongAdder();
	private final LongAdder saturatedMax = new LongAdder();
	
	public MetricsLayerRecorder(Layer layer) {
		this.layer = layer;
	}
	
	MetricsLayerRecorder neuronFired(AbstractNeuron neuron) {
		spikeCounter.increment();
		activeNeurons.add(neuron);
		return this;
	}

	MetricsLayerRecorder updateSynapticStatistics(Synapse source, SynapseUpdatedData data) {
		absWeightSum.add(Maths.abs(data.newValue() - data.oldValue()));
		float oldW = data.oldValue();
		float newW = data.newValue();

		sumWeights.add(newW - oldW);
		sumWeights2.add(newW*newW - oldW*oldW);

		ExcitatorySynapticPlasticityConfig cfg = layer.getConfig()
				.SYNAPSE_PLASTICITY_CONFIG
				.excitatory();		
		
		boolean oldMin = oldW <= cfg.W_MIN + 1e-6f;
		boolean newMin = newW <= cfg.W_MIN + 1e-6f;

		if (oldMin && !newMin) saturatedMin.decrement();
		if (!oldMin && newMin) saturatedMin.increment();

		boolean oldMax = oldW >= cfg.W_MAX - 1e-6f;
		boolean newMax = newW >= cfg.W_MAX - 1e-6f;

		if (oldMax && !newMax) saturatedMax.decrement();
		if (!oldMax && newMax) saturatedMax.increment();
		return this;
	}
	
	public LayerStats getStatsAndReset() {
		//long start = System.nanoTime();
		long spikesCount = spikeCounter.sumThenReset();
		int activeCount = activeNeurons.size();
		int totalNeurons = layer.getNeuronsCount();

		double avgFiringRateActive = activeCount == 0 ? 0.0 :
			(double) spikesCount / activeCount;

		double avgFiringRateAll = (double) spikesCount / totalNeurons;
		double sparsity = 1.0 - ((double) activeCount / totalNeurons);

		long count = layer.getSynapsesCount();
		double sum = sumWeights.sum();   // NON reset
		double sum2 = sumWeights2.sum(); // NON reset
		double satMin = saturatedMin.sumThenReset();
		double satMax = saturatedMax.sumThenReset();
		
		double averageSynapticWeight = count == 0 ? 0.0 : sum / count;
		double variance = count == 0 ? 0.0 : (sum2 / count) - (averageSynapticWeight * averageSynapticWeight);
		double synapticWeightStdDev = Maths.sqrt(Maths.max(variance, 0.0));

		double saturatedMinRatio = count == 0 ? 0.0 : (double) satMin / count;
		double saturatedMaxRatio = count == 0 ? 0.0 : (double) satMax / count;

		double totalPlasticity = absWeightSum.sumThenReset();
		double energy = spikesCount * Maths.abs(averageSynapticWeight + synapticWeightStdDev);

		// ---------------------------------------------------------
		//  STABILITÀ ATTIVAZIONE
		// ---------------------------------------------------------
		int overlap = 0;
		for (AbstractNeuron n : activeNeurons) {
			if (prevActive.contains(n)) overlap++;
		}

		double activationStability = activeCount == 0 ? 0.0 :
			(double) overlap / activeCount;

		prevActive.clear();
		prevActive.addAll(activeNeurons);
		activeNeurons.clear();

		//long end = System.nanoTime();
		//logger.info("Layer:{} metrics recorded in:{}micros", layer.getLayerId(), TimeUnit.NANOSECONDS.toMicros(end-start));
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
