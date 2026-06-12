package com.cortex.brain;

import static com.cortex.base.Commons.ALWAYS_CONNECT_PREDICATE;
import static com.cortex.base.Commons.ONLY_INHIBITOR_CONNECT_PREDICATE;

import java.util.ArrayList;
import java.util.List;

import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.base.plasticity.SynapsePlasticityConfig;

public class BrainLayersConnConfig {
	private final int connScale;
	
	// baseSpeed = nanoseconds per unit distance
	// FAST  ≈ 2.5 ms per unit
	// MID   ≈ 3.0 ms per unit
	// SLOW  ≈ 4.0 ms per unit
	// delay = length * baseSpeed / (1 + myelinFactor)
	public static enum SYNAPSE_SPEED {
		FAST(2_500_000l),
		MID (3_000_000l),
		SLOW(4_000_000l);

		private final long baseSpeed;
		
		SYNAPSE_SPEED(long l) {
			this.baseSpeed = l;
		}

		public long getBaseSpeed() {
			return baseSpeed;
		}
	}
	
	public static SynapsePlasticityConfig ffPlasticity() {
		return new SynapsePlasticityConfig(
				ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.0030f, 0.0015f, 60_000_000L, 120_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.32f, 0.45f, 0.12f, 0.30f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibilityDecaySeconds(0.995f) 				// ELIGIBILITY_DECAY
				.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.003f) 			// HOMEOSTATIC_RATE
				.build(),
				InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.40f, 1.5f, 0.10f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.002f)				// LEARNING_RATE
				.withTargetFiringRate(8.0f)				// TARGET_FIRING_RATE
				.build()
				);
	}

	public static SynapsePlasticityConfig fbPlasticity() {
		return new SynapsePlasticityConfig(
				ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibilityDecaySeconds(0.990f) 				// ELIGIBILITY_DECAY
				.withPlasticity(80f, 10f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.001f) 			// HOMEOSTATIC_RATE
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
				.withEligibilityDecaySeconds(0.995f) 				// ELIGIBILITY_DECAY
				.withPlasticity(150f, 20f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.001f) 			// HOMEOSTATIC_RATE
				.build(),
				InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.80f, 1.5f, 0.2f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.00005f)				// LEARNING_RATE
				.withTargetFiringRate(4.0f)				// TARGET_FIRING_RATE
				.build()
				);
	}

	public List<LayerConnectionsConfig> getLayersConnectionsConfig(List<? extends Abstract3DLayer> layers) {
		List<LayerConnectionsConfig> cfg = new ArrayList<>();
		Abstract3DLayer L0 = layers.get(0);
		Abstract3DLayer L1 = layers.get(1);
		Abstract3DLayer L2 = layers.get(2);
		Abstract3DLayer L3 = layers.get(3);
		Abstract3DLayer L4 = layers.get(4);
		Abstract3DLayer L5 = layers.get(5);

		// L0 -> L1
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L0).to(L1).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(1.5*connScale), (int)(3.0*connScale), 0.45f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L0 -> L2
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L0).to(L2).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.6*connScale), (int)(1.2*connScale), 0.85f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L1 -> L2
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L1).to(L2).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.4*connScale), (int)(0.6*connScale), 0.50f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L1 -> L3
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L1).to(L3).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(1.4*connScale), (int)(1.8*connScale), 0.80f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L1 -> L4
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L1).to(L4).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.2*connScale), (int)(0.5*connScale), 0.85f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());
/*
		// L1 -> L0 (feedback)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L1).to(L0).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.3*connScale), (int)(0.5*connScale), 0.50f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());
*/
		// L2 -> L3
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L2).to(L3).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(7.0*connScale), (int)(10.0*connScale), 0.75f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());
/*
		// L2 -> L1 (feedback, solo inibitori)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L2).to(L1).withBaseSped(SYNAPSE_SPEED.SLOW.getBaseSpeed())
				.withConnections((int)(0.5*connScale), (int)(0.9*connScale), 0.50f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());
*/
		// L2 -> L4
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L2).to(L4).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.6*connScale), 0.50f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L3 -> L4 (controllo, solo inibitori)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L3).to(L4).withBaseSped(SYNAPSE_SPEED.SLOW.getBaseSpeed())
				.withConnections((int)(2.0*connScale), (int)(3.5*connScale), 0.30f)
				.withSynapsePlasticityConfig(ctrlPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());
/*
		// L3 -> L2 (feedback)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L3).to(L2).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.4*connScale), 0.50f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());

		// L3 -> L1 (feedback)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L3).to(L1).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.4*connScale), 0.60f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());
*/
		// L3 -> L5 (controllo)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L3).to(L5).withBaseSped(SYNAPSE_SPEED.SLOW.getBaseSpeed())
				.withConnections((int)(0.6*connScale), (int)(1.8*connScale), 0.80f)
				.withSynapsePlasticityConfig(ctrlPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());
/*
		// L4 -> L2 (feedback)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L4).to(L2).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.2*connScale), (int)(0.8*connScale), 0.80f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());

		// L5 -> L3
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L5).to(L3).withBaseSped(SYNAPSE_SPEED.SLOW.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.2*connScale), 1.0f)
				.withSynapsePlasticityConfig(ctrlPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L5 -> L2
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L5).to(L2).withBaseSped(SYNAPSE_SPEED.SLOW.getBaseSpeed())
				.withConnections((int)(0.8*connScale), (int)(2.0*connScale), 1.0f)
				.withSynapsePlasticityConfig(ctrlPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());

		// L5 -> L1
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L5).to(L1).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.5*connScale), (int)(1.5*connScale), 1.0f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());

		// L5 -> L0
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L5).to(L0).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.4*connScale), 1.0f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());
*/
		// L4 -> L5
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L4).to(L5).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.5*connScale), (int)(1.8*connScale), 0.40f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L3 -> L3 (ricorrenza locale)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L3).to(L3).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.2*connScale), (int)(0.6*connScale), 0.40f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());
/*
		// L4 -> L1 (feedback modulante)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L4).to(L1).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.2*connScale), (int)(0.6*connScale), 0.70f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());

		// L4 -> L0 (feedback debole)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L4).to(L0).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.2*connScale), 0.90f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());

		// L2 -> L0 (feedback medio)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L2).to(L0).withBaseSped(SYNAPSE_SPEED.MID.getBaseSpeed())
				.withConnections((int)(0.1*connScale), (int)(0.4*connScale), 0.75f)
				.withSynapsePlasticityConfig(fbPlasticity())
				.withNeuronFilter(ONLY_INHIBITOR_CONNECT_PREDICATE)
				.build());
*/
		// L2 -> L5 (feedforward verso controllo)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L2).to(L5).withBaseSped(SYNAPSE_SPEED.SLOW.getBaseSpeed())
				.withConnections((int)(0.2*connScale), (int)(0.6*connScale), 0.60f)
				.withSynapsePlasticityConfig(ctrlPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		// L1 -> L5 (feedforward debole)
		cfg.add(LayerConnectionsConfig.newBuilder()
				.from(L1).to(L5).withBaseSped(SYNAPSE_SPEED.FAST.getBaseSpeed())
				.withConnections((int)(0.2*connScale), (int)(0.3*connScale), 0.90f)
				.withSynapsePlasticityConfig(ffPlasticity())
				.withNeuronFilter(ALWAYS_CONNECT_PREDICATE)
				.build());

		return cfg;
	}

	public BrainLayersConnConfig(int connScale) {
		super();
		this.connScale = connScale;
	}
}
