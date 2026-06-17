package com.cortex.base.layers;

import static com.cortex.base.Commons.ALWAYS_CONNECT_PREDICATE;

import com.cortex.base.lateral_inhibition.CombinedLateralInhibition;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.brain.CorticalNeuronsConfig;;

public class MultiLayersConfig {

	public LayerConfig l0_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(0)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.30f)
	        .withCombinedLateralInhibition(new CombinedLateralInhibition(1.0f, 1.0f, 1.0f, 0.2f))
	        .withDimension(1.00f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.15f)
	            .withRatePerSecond(50_000_000L, 0.98f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.010f, 0.009f, 60_000_000L, 120_000_000L)
	                .withWeights(0.25f, 0.50f, 0.05f, 0.20f)
	                .withEligibilityDecaySeconds(0.995f)
	                .withPlasticity(30f, 5f)
	                .withHomeostaticRate(0.010f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.80f, 3.0f, 0.2f)
	                .withLearningRate(0.005f)
	                .withTargetFiringRate(2.5f)
	                .build()
	        )).build();
	}

	public LayerConfig l1_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(1)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.25f)
	        .withCombinedLateralInhibition(new CombinedLateralInhibition(1.0f, 1.0f, 0.7f, 0.5f))
	        .withDimension(0.85f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.12f)
	            .withRatePerSecond(50_000_000L, 0.90f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0030f, 0.0015f, 60_000_000L, 120_000_000L)
	                .withWeights(0.22f, 0.38f, 0.10f, 0.20f)
	                .withEligibilityDecaySeconds(0.995f)
	                .withPlasticity(25f, 5f)
	                .withHomeostaticRate(0.0015f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.35f, 1.5f, 0.10f)
	                .withLearningRate(0.002f)
	                .withTargetFiringRate(6.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l2_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(2)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.18f)
	        .withCombinedLateralInhibition(new CombinedLateralInhibition(1.0f, 1.0f, 0.4f, 0.8f))
	        .withDimension(0.70f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.70f)
	            .withRatePerSecond(50_000_000L, 0.35f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0020f, 0.0015f, 70_000_000L, 120_000_000L)
	                .withWeights(0.08f, 0.20f, 0.04f, 0.08f)
	                .withEligibilityDecaySeconds(0.993f)
	                .withPlasticity(5f, 1f)
	                .withHomeostaticRate(0.002f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.90f, 2.2f, 0.30f)
	                .withLearningRate(0.0012f)
	                .withTargetFiringRate(5.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l3_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(3)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.40f)
	        .withCombinedLateralInhibition(new CombinedLateralInhibition(1.0f, 1.0f, 0.2f, 1.0f))
	        .withDimension(0.55f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(1.50f)
	            .withRatePerSecond(50_000_000L, 0.45f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0020f, 0.0010f, 70_000_000L, 120_000_000L)
	                .withWeights(0.04f, 0.12f, 0.01f, 0.05f)
	                .withEligibilityDecaySeconds(0.990f)
	                .withPlasticity(20f, 4f)
	                .withHomeostaticRate(0.010f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(1.80f, 3.50f, 0.50f) 
	                .withLearningRate(0.0025f)
	                .withTargetFiringRate(2.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l4_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(4)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.22f)
	        .withCombinedLateralInhibition(new CombinedLateralInhibition(1.0f, 1.0f, 0.1f, 1.0f))
	        .withDimension(0.40f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(1.00f)
	            .withRatePerSecond(90_000_000L, 0.60f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0025f, 0.0018f, 80_000_000L, 150_000_000L)
	                .withWeights(0.06f, 0.18f, 0.02f, 0.08f)
	                .withEligibilityDecaySeconds(0.992f)
	                .withPlasticity(40f, 7f)
	                .withHomeostaticRate(0.001f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(1.20f, 2.50f, 0.30f)
	                .withLearningRate(0.0025f)
	                .withTargetFiringRate(3.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l5_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(5)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.15f)
	        .withCombinedLateralInhibition(new CombinedLateralInhibition(1.0f, 1.0f, 0.0f, 1.0f))
	        .withDimension(0.20f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(1.20f)
	            .withRatePerSecond(120_000_000L, 0.60f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0035f, 0.0022f, 80_000_000L, 150_000_000L)
	                .withWeights(0.08f, 0.22f, 0.03f, 0.10f)
	                .withEligibilityDecaySeconds(0.991f)
	                .withPlasticity(70f, 12f)
	                .withHomeostaticRate(0.004f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(1.40f, 2.80f, 0.40f)
	                .withLearningRate(0.0018f)
	                .withTargetFiringRate(4.0f)
	                .build()
	        )).build();
	}
}
