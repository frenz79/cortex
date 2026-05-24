package com.cortex.brain;

import static com.cortex.base.Synapse.ALWAYS_CONNECT_PREDICATE;

import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.config.InhibitorySynapticPlasticityConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;;

public class BrainLayersConfig {

	//	(int)(totN*0.15), 0.15f,  5*connScale, 7*connScale, 0.60f, 1.00f, true, true,
	public LayerConfig l0_config( int neuronsCount, int minConnections, int maxConnections, float maxConnDistance ) {
		return	LayerConfig.newBuilder(0)
				.enableInConn(true)
				.enableOutConn(true)
				.withInhibitorFreq(0.60f)				
				.withDimension(1.00f)
				.withNeurons(neuronsCount)
				.withConnection(minConnections, maxConnections, maxConnDistance)
				.withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
				.withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
					.withFiringThreshold(0.10f)
					.withRepolarizationPerSecond(0.10f)
					.withRate(50_000_000l, 0.98f)
					.build()
				)
				.withSynapsePlasticityConfig(
						new SynapsePlasticityConfig(	
								ExcitatorySynapticPlasticityConfig.newBuilder()
								.withSTDP(0.01f, 0.009f, 60_000_000L, 120_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
								.withWeights(0.25f, 0.50f, 0.05f, 0.20f)	// INITIAL, W_MAX, W_MIN, W_BASELINE
								.withEligibility(0.995f) 					// ELIGIBILITY_DECAY
								.withPlasticity(30f, 5f)					// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
								.withHomeostaticRate(0.01f) 				// HOMEOSTATIC_RATE
								.withInitialDelay(1.2f)
								.build(),			
								InhibitorySynapticPlasticityConfig.newBuilder()
								.withWeights(0.60f, 2.5f, 0.15f)			// INITIAL, W_MAX, W_MIN,
								.withLearningRate(0.003f)					// LEARNING_RATE
								.withTargetFiringRate(3.0f)				 	// TARGET_FIRING_RATE
								.build()	
								)
						).build();
	}

	public LayerConfig l1_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
		return LayerConfig.newBuilder(1)
				.enableInConn(true)
				.enableOutConn(true)
				.withInhibitorFreq(0.25f)
				.withDimension(0.85f)
				.withNeurons(neuronsCount)
				.withConnection(minConnections, maxConnections, maxConnDistance)
				.withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
				.withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
					.withFiringThreshold(0.25f)
					.withRepolarizationPerSecond(0.3f)
					.withRate(50_000_000l, 0.95f)
					.build()
				)
				.withSynapsePlasticityConfig(
						new SynapsePlasticityConfig(
								ExcitatorySynapticPlasticityConfig.newBuilder()
								.withSTDP(0.002f, 0.002f, 50_000_000L, 50_000_000L)
								.withWeights(0.20f, 0.30f, 0.10f, 0.20f)
								.withEligibility(0.990f)
								.withPlasticity(40f, 5f)
								.withHomeostaticRate(0.002f)
								.withInitialDelay(1.0f)
								.build(),
								InhibitorySynapticPlasticityConfig.newBuilder()
								.withWeights(0.50f, 3.0f, 0.2f)
								.withLearningRate(0.005f)
								.withTargetFiringRate(2.5f)
								.build()
								)
						).build();
	}

	public LayerConfig l2_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
		return LayerConfig.newBuilder(2)
				.enableInConn(true)
				.enableOutConn(true)
				.withInhibitorFreq(0.15f)
				.withDimension(0.65f)
				.withNeurons(neuronsCount)
				.withConnection(minConnections, maxConnections, maxConnDistance)
				.withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
				.withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
					.withFiringThreshold(0.25f)
					.withRepolarizationPerSecond(0.3f)
					.withRate(50_000_000l, 0.95f)
					.build()
				)
				.withSynapsePlasticityConfig(
						new SynapsePlasticityConfig(
								ExcitatorySynapticPlasticityConfig.newBuilder()
								.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L)
								.withWeights(0.20f, 0.30f, 0.10f, 0.25f)
								.withEligibility(0.990f)
								.withPlasticity(20f, 1f)
								.withHomeostaticRate(0.008f)
								.withInitialDelay(1.2f)
								.build(),
								InhibitorySynapticPlasticityConfig.newBuilder()
								.withWeights(0.80f, 3.0f, 0.2f)
								.withLearningRate(0.005f)
								.withTargetFiringRate(2.5f)
								.build()
								)
						).build();
	}

	public LayerConfig l3_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
		return LayerConfig.newBuilder(3)
				.enableInConn(true)
				.enableOutConn(true)
				.withInhibitorFreq(0.30f)
				.withDimension(0.45f)
				.withNeurons(neuronsCount)
				.withConnection(minConnections, maxConnections, maxConnDistance)
				.withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
				.withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
					.withFiringThreshold(0.35f)
					.withRepolarizationPerSecond(0.2f)
					.withRate(100_000_000l, 0.95f)
					.build()
				)
				.withSynapsePlasticityConfig(
						new SynapsePlasticityConfig(
								ExcitatorySynapticPlasticityConfig.newBuilder()
								.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L)
								.withWeights(0.20f, 0.30f, 0.10f, 0.20f)
								.withEligibility(0.990f)
								.withPlasticity(20f, 1f)
								.withHomeostaticRate(0.002f)
								.withInitialDelay(1.2f)
								.build(),
								InhibitorySynapticPlasticityConfig.newBuilder()
								.withWeights(0.60f, 3.0f, 0.2f)
								.withLearningRate(0.005f)
								.withTargetFiringRate(2.5f)
								.build()
								)
						).build();
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
					.withFiringThreshold(0.25f)
					.withRepolarizationPerSecond(0.2f)
					.withRate(100_000_000l, 0.95f)
					.build()
				)
				.withSynapsePlasticityConfig(
						new SynapsePlasticityConfig(
								ExcitatorySynapticPlasticityConfig.newBuilder()
								.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L)
								.withWeights(0.15f, 0.30f, 0.10f, 0.20f)
								.withEligibility(0.985f)
								.withPlasticity(80f, 10f)
								.withHomeostaticRate(0.002f)
								.withInitialDelay(1.5f)
								.build(),
								InhibitorySynapticPlasticityConfig.newBuilder()
								.withWeights(0.60f, 3.0f, 0.2f)
								.withLearningRate(0.005f)
								.withTargetFiringRate(2.5f)
								.build()
								)
						).build();
	}

	public LayerConfig l5_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
		return LayerConfig.newBuilder(5)
				.enableInConn(true)
				.enableOutConn(true)
				.withInhibitorFreq(0.20f)
				.withDimension(0.15f)
				.withNeurons(neuronsCount)
				.withConnection(minConnections, maxConnections, maxConnDistance)
				.withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
				.withCorticalNeuronsConfig(CorticalNeuronsConfig.newBuilder()
					.withFiringThreshold(0.45f)
					.withRepolarizationPerSecond(0.15f)
					.withRate(150_000_000l, 0.95f)
					.build()
				)
				.withSynapsePlasticityConfig(
						new SynapsePlasticityConfig(
								ExcitatorySynapticPlasticityConfig.newBuilder()
								.withSTDP(0.004f, 0.003f, 80_000_000L, 150_000_000L)
								.withWeights(0.12f, 0.30f, 0.10f, 0.20f)
								.withEligibility(0.99f)
								.withPlasticity(20f, 1f)
								.withHomeostaticRate(0.01f)
								.withInitialDelay(1.5f)
								.build(),
								InhibitorySynapticPlasticityConfig.newBuilder()
								.withWeights(0.20f, 3.0f, 0.15f)
								.withLearningRate(0.005f)
								.withTargetFiringRate(2.5f)
								.build()
								)
						).build();
	}
}
