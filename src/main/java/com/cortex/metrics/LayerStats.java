package com.cortex.metrics;

import com.cortex.base.layers.AbstractLayer;

public record LayerStats( 
		AbstractLayer layer,
		
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