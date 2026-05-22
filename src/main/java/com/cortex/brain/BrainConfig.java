package com.cortex.brain;

import java.util.function.Predicate;

import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.config.InhibitorySynapticPlasticityConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;

public class BrainConfig {

	private static final Predicate<CorticalNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	private static final Predicate<CorticalNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	private static final Predicate<CorticalNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = CorticalNeuron::isInhibitor;

	public static SynapsePlasticityConfig ffPlasticity() {
	    return new SynapsePlasticityConfig(
	    	ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.20f, 0.30f, 0.09f, 0.25f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
				.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
				.withInitialDelay(1.2f)
				.build(),
			InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.005f)				// LEARNING_RATE
				.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
				.build()
	    );
	}

	public static SynapsePlasticityConfig fbPlasticity() {
		return new SynapsePlasticityConfig(
			ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
				.withPlasticity(80f, 10f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.001f) 			// HOMEOSTATIC_RATE
				.withInitialDelay(1.2f)
				.build(),
			InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.00005f)				// LEARNING_RATE
				.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
				.build()
	    );
	}

	public static SynapsePlasticityConfig ctrlPlasticity() {
		return new SynapsePlasticityConfig(
			ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.004f, 0.003f, 150_000_000L, 300_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibility(0.995f) 				// ELIGIBILITY_DECAY
				.withPlasticity(150f, 20f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.001f) 			// HOMEOSTATIC_RATE
				.withInitialDelay(1.2f)
				.build(),
			InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.80f, 1.5f, 0.2f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.00005f)				// LEARNING_RATE
				.withTargetFiringRate(4.0f)				// TARGET_FIRING_RATE
				.build()
		   );
	}
	
	//	(int)(totN*0.15), 0.15f,  5*connScale, 7*connScale, 0.60f, 1.00f, true, true,
	public static LayerConfig l0_config( int neuronsCount, int minConnections, int maxConnections, float maxConnDistance ) {
		return	LayerConfig.newBuilder(0)
				.enableInConn(true)
				.enableOutConn(true)
				.withInhibitorFreq(0.60f)				
				.withDimension(1.00f)
				.withNeurons(neuronsCount)
				.withConnection(minConnections, maxConnections, maxConnDistance)
				.withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
				.withSynapsePlasticityConfig(
					new SynapsePlasticityConfig(	
						ExcitatorySynapticPlasticityConfig.newBuilder()
							.withSTDP(0.003f, 0.0025f, 60_000_000L, 120_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
							.withWeights(0.25f, 0.35f, 0.12f, 0.25f)	// INITIAL, W_MAX, W_MIN, W_BASELINE
							.withEligibility(0.992f) 					// ELIGIBILITY_DECAY
							.withPlasticity(30f, 5f)					// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
							.withHomeostaticRate(0.003f) 				// HOMEOSTATIC_RATE
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
		
	public static LayerConfig l1_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(1)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.25f)
	        .withDimension(0.85f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
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
	
	public static LayerConfig l2_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(2)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.15f)
	        .withDimension(0.65f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
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
	
	public static LayerConfig l3_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(3)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.30f)
	        .withDimension(0.45f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
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
	
	public static LayerConfig l4_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(4)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.10f)
	        .withDimension(0.30f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
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
	
	public static LayerConfig l5_config(int neuronsCount, int minConnections, int maxConnections, float maxConnDistance) {
	    return LayerConfig.newBuilder(5)
	        .enableInConn(true)
	        .enableOutConn(true)
	        .withInhibitorFreq(0.20f)
	        .withDimension(0.15f)
	        .withNeurons(neuronsCount)
	        .withConnection(minConnections, maxConnections, maxConnDistance)
	        .withConnectionFilter(ALWAYS_CONNECT_PREDICATE)
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
	
	
	// L0 -> L1		
			cfg.addIntraLayerConfig(0, 1, new IntraLayersConnConfig(
					(int)(1.5*connScale), (int)(3.0*connScale), 
					0.45f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L0 -> L2
			cfg.addIntraLayerConfig(0, 2, new IntraLayersConnConfig(
					(int)(0.6*connScale), (int)(1.2*connScale), 
					0.85f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L1 -> L2
			cfg.addIntraLayerConfig(1, 2, new IntraLayersConnConfig(
					(int)(0.4*connScale), (int)(0.6*connScale), 
					0.50f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L1 -> L3
			cfg.addIntraLayerConfig(1, 3, new IntraLayersConnConfig(
					(int)(0.8*connScale), (int)(1.2*connScale), 
					0.9f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L1 -> L4
			cfg.addIntraLayerConfig(1, 4, new IntraLayersConnConfig(
					(int)(0.2*connScale), (int)(0.5*connScale), 
					0.85f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L1 -> L0
			cfg.addIntraLayerConfig(1, 0, new IntraLayersConnConfig(
					(int)(0.3*connScale), (int)(0.5*connScale), 
					0.50f, // max distance
					fbPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L2 -> L3
			cfg.addIntraLayerConfig(2, 3, new IntraLayersConnConfig(
					(int)(1.5*connScale), (int)(4.0*connScale), 
					0.50f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L2 -> L1
			cfg.addIntraLayerConfig(2, 1, new IntraLayersConnConfig(
					(int)(0.5*connScale), (int)(1.5*connScale), 
					0.50f, // max distance
					fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L2 -> L4
			cfg.addIntraLayerConfig(2, 4, new IntraLayersConnConfig(
					(int)(0.1*connScale), (int)(0.6*connScale), 
					0.50f, // max distance
					ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
			// L3 -> L4
			cfg.addIntraLayerConfig(3, 4, new IntraLayersConnConfig(
					(int)(1.0*connScale), (int)(3.0*connScale), 
					0.25f, // max distance
					ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L3 -> L2
			cfg.addIntraLayerConfig(3, 2, new IntraLayersConnConfig(
					(int)(0.1*connScale), (int)(0.4*connScale), 
					0.50f, // max distance
					fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L3 -> L1
			cfg.addIntraLayerConfig(3, 1, new IntraLayersConnConfig(
					(int)(0.1*connScale), (int)(0.4*connScale), 
					0.60f, // max distance
					fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L3 -> L5
			cfg.addIntraLayerConfig(3, 5, new IntraLayersConnConfig(
					(int)(0.2*connScale), (int)(0.8*connScale), 
					0.40f,
					ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L4 -> L3
			//cfg.addIntraLayerConfig(4, 3, new IntraLayersConnConfig(
			//		(int)(0.3*connScale), (int)(1.0*connScale), 
			//		0.50f, // max distance
			//		ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L4 -> L2
			cfg.addIntraLayerConfig(4, 2, new IntraLayersConnConfig(
					(int)(0.2*connScale), (int)(0.8*connScale), 
					0.80f, // max distance
					fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L5 -> L3
			cfg.addIntraLayerConfig(5, 3, new IntraLayersConnConfig(
					(int)(0.1*connScale), (int)(0.2*connScale), 
					1.0f, // max distance
					ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L5 -> L2
			cfg.addIntraLayerConfig(5, 2, new IntraLayersConnConfig(
					(int)(0.8*connScale), (int)(2.0*connScale), 
					1.0f, // max distance
					ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L5 -> L1
			cfg.addIntraLayerConfig(5, 1, new IntraLayersConnConfig(
					(int)(0.5*connScale), (int)(1.5*connScale), 
					1.0f, // max distance
					fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
			// L5 -> L0
			cfg.addIntraLayerConfig(5, 0, new IntraLayersConnConfig(
					(int)(0.1*connScale), (int)(0.4*connScale), 
					1.0f, // max distance
					fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));

			
			// L3 -> L4
			cfg.addIntraLayerConfig(3, 4, new IntraLayersConnConfig(
					(int)(0.2*connScale), (int)(0.8*connScale), 
					0.40f,
					ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
			// L4 -> L5
			cfg.addIntraLayerConfig(4, 5, new IntraLayersConnConfig(
					(int)(0.2*connScale), (int)(0.8*connScale), 
					0.40f,
					ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
			// L3 -> L3
			cfg.addIntraLayerConfig(3, 3, new IntraLayersConnConfig(
				    (int)(0.2*connScale), (int)(0.6*connScale),
				    0.40f,
				    ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
			
			// L4 -> L1  (feedback modulante)
			cfg.addIntraLayerConfig(4, 1, new IntraLayersConnConfig(
			        (int)(0.2*connScale), (int)(0.6*connScale),
			        0.70f, // max distance
			        fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			
			// L4 -> L0  (feedback molto debole)
			cfg.addIntraLayerConfig(4, 0, new IntraLayersConnConfig(
			        (int)(0.05*connScale), (int)(0.2*connScale),
			        0.90f, // max distance
			        fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			
			// L2 -> L0  (feedback medio)
			cfg.addIntraLayerConfig(2, 0, new IntraLayersConnConfig(
			        (int)(0.1*connScale), (int)(0.4*connScale),
			        0.75f,
			        fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
			
			// L2 -> L5  (feedforward verso controllo)
			cfg.addIntraLayerConfig(2, 5, new IntraLayersConnConfig(
			        (int)(0.2*connScale), (int)(0.6*connScale),
			        0.60f,
			        ctrlPlasticity(), ALWAYS_CONNECT_PREDICATE));
			
			// L1 -> L5  (feedforward debole)
			cfg.addIntraLayerConfig(1, 5, new IntraLayersConnConfig(
			        (int)(0.1*connScale), (int)(0.3*connScale),
			        0.70f,
			        ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
}
