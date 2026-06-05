package com.cortex.brain;

import static com.cortex.base.Synapse.ALWAYS_CONNECT_PREDICATE;

import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.config.InhibitorySynapticPlasticityConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;;

public class BrainLayersConfig {

	public LayerConfig l0_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(0)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.30f)
	        .withDimension(1.00f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.10f)
	            .withRepolarizationPerSecond(0.10f)
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
	                .withInitialDelay(1.2f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.60f, 2.5f, 0.15f)
	                .withLearningRate(0.0015f)
	                .withTargetFiringRate(6.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l1_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(1)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.20f)
	        .withDimension(0.85f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.070f)
	            .withRepolarizationPerSecond(0.10f)
	            .withRatePerSecond(50_000_000L, 0.97f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0030f, 0.0015f, 60_000_000L, 120_000_000L)
	                .withWeights(0.22f, 0.38f, 0.10f, 0.20f)
	                .withEligibilityDecaySeconds(0.995f)
	                .withPlasticity(25f, 5f)
	                .withHomeostaticRate(0.0015f)
	                .withInitialDelay(1.0f)
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
	        .withInhibitorFreq(0.10f)
	        .withDimension(0.65f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.045f)
	            .withRepolarizationPerSecond(0.18f)
	            .withRatePerSecond(50_000_000L, 0.85f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0020f, 0.0015f, 70_000_000L, 120_000_000L)
	                .withWeights(0.45f, 0.55f, 0.12f, 0.30f)
	                .withEligibilityDecaySeconds(0.993f)
	                .withPlasticity(15f, 3f)
	                .withHomeostaticRate(0.0005f)
	                .withInitialDelay(0.3f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.30f, 1.4f, 0.10f)
	                .withLearningRate(0.0012f)
	                .withTargetFiringRate(18.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l3_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(3)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.10f)
	        .withDimension(0.60f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.045f)
	            .withRepolarizationPerSecond(0.10f)
	            .withRatePerSecond(50_000_000L, 0.95f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0035f, 0.0020f, 70_000_000L, 120_000_000L)
	                .withWeights(0.40f, 0.58f, 0.14f, 0.36f)
	                .withEligibilityDecaySeconds(0.993f)
	                .withPlasticity(30f, 5f)
	                .withHomeostaticRate(0.004f)
	                .withInitialDelay(0.1f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.30f, 1.2f, 0.10f)
	                .withLearningRate(0.0008f)
	                .withTargetFiringRate(10.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l4_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(4)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.10f)
	        .withDimension(0.30f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.05f)
	            .withRepolarizationPerSecond(0.08f)
	            .withRatePerSecond(100_000_000L, 0.95f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0030f, 0.0020f, 80_000_000L, 150_000_000L)
	                .withWeights(0.22f, 0.40f, 0.12f, 0.25f)
	                .withEligibilityDecaySeconds(0.990f)
	                .withPlasticity(60f, 10f)
	                .withHomeostaticRate(0.002f)
	                .withInitialDelay(1.5f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.25f, 1.5f, 0.10f)
	                .withLearningRate(0.002f)
	                .withTargetFiringRate(6.0f)
	                .build()
	        )).build();
	}

	public LayerConfig l5_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(5)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.05f)
	        .withDimension(0.15f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
	        .withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
	            .withFiringThreshold(0.05f)
	            .withRepolarizationPerSecond(0.08f)
	            .withRatePerSecond(150_000_000L, 0.95f)
	            .build()
	        )
	        .withSynapsePlasticityConfig(new SynapsePlasticityConfig(
	            ExcitatorySynapticPlasticityConfig.newBuilder()
	                .withSTDP(0.0030f, 0.0020f, 80_000_000L, 150_000_000L)
	                .withWeights(0.30f, 0.55f, 0.15f, 0.30f)
	                .withEligibilityDecaySeconds(0.990f)
	                .withPlasticity(60f, 10f)
	                .withHomeostaticRate(0.002f)
	                .withInitialDelay(1.5f)
	                .build(),
	            InhibitorySynapticPlasticityConfig.newBuilder()
	                .withWeights(0.20f, 1.5f, 0.10f)
	                .withLearningRate(0.002f)
	                .withTargetFiringRate(6.0f)
	                .build()
	        )).build();
	}
}
