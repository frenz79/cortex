package com.cortex.globals;

import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.brain.layers.Layer;
import com.cortex.globals.EventBus.EventListener;
import com.cortex.globals.EventBus.EventType;
import com.cortex.metrics.LayerStats;

public class DiscreteAdaptiveStabilizer {

	private final DiscreteAdaptiveStabilizerConfig config;

	public DiscreteAdaptiveStabilizer( DiscreteAdaptiveStabilizerConfig config ) {
		this.config = config;

		EventBus.addListener(EventType.LAYER_STATS,  new EventListener() {

			@Override
			public void onEvent(EventType type, long time, Object source, Object data) {
				LayerStats stats = (LayerStats)data;
				if (stats!=null) {
					adaptLayer( stats );
				}
			}
		});
	}

	private void adaptLayer(LayerStats stats) {
		Layer layer = stats.layer();
		int lid = layer.getLayerId();

		CorticalNeuronsConfig ncfg =
				layer.getConfig().CORTICAL_NEURONS_CONFIG;

		ExcitatorySynapticPlasticityConfig pcfg =
				layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatorySynapticPlasticityConfig();
		
		if (lid == 0) {
		    // revival se morto
		    if (stats.activeNeurons() == 0) {
		        ncfg.FIRING_THRESHOLD -= 0.05f;
		        ncfg.REPOLARIZATION_PER_SECOND -= 0.05f;
		    }

		    // clamp minimi
		    ncfg.FIRING_THRESHOLD = Math.max(0.05f, ncfg.FIRING_THRESHOLD);
		    ncfg.REPOLARIZATION_PER_SECOND = Math.max(0.01f, ncfg.REPOLARIZATION_PER_SECOND);

		    return; // IMPORTANTISSIMO: NON TOCCARE ALTRO
		}
		
		// ---------------------------------------------------------
		// 1) Recupero parametri per-layer (override)
		// ---------------------------------------------------------
		DiscreteAdaptiveStabilizerConfig.LayerAdaptiveParams lp =
				config.perLayer.get(lid);

		float firingLow   = (lp != null ? lp.TARGET_FIRING_LOW   : config.TARGET_FIRING_LOW);
		float firingHigh  = (lp != null ? lp.TARGET_FIRING_HIGH  : config.TARGET_FIRING_HIGH);
		float sparsityMin = (lp != null ? lp.TARGET_SPARSITY_MIN : config.TARGET_SPARSITY_MIN);
		float sparsityMax = (lp != null ? lp.TARGET_SPARSITY_MAX : config.TARGET_SPARSITY_MAX);
		float maxEnergy   = (lp != null ? lp.MAX_ENERGY          : config.MAX_ENERGY_PER_LAYER);

		// ---------------------------------------------------------
		// 2) Lettura metriche
		// ---------------------------------------------------------
		double firingAll   = stats.avgFiringRateAll();
		double sparsity    = stats.sparsity();
		double satMaxRatio = stats.saturatedMaxRatio();
		double satMinRatio = stats.saturatedMinRatio();
		double energy      = stats.energy();
		double stability   = stats.activationStability();
		double plasticity  = stats.totalPlasticity();

		// ---------------------------------------------------------
		// 3) Protezione L0 (mai spegnerlo)
		// ---------------------------------------------------------
		if (lid == 0) {
			ncfg.FIRING_THRESHOLD = Math.min(ncfg.FIRING_THRESHOLD, 0.6f);
		}
		
		if (lid == 0)
		    ncfg.REPOLARIZATION_PER_SECOND = Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.15f);
		else
		    ncfg.REPOLARIZATION_PER_SECOND = Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.3f);

		// ---------------------------------------------------------
		// 4) Protezione layer profondi (mai troppo attivi)
		// ---------------------------------------------------------
		if (lid >= 3) {
			ncfg.FIRING_THRESHOLD = Math.max(ncfg.FIRING_THRESHOLD, 0.5f);
		}

		// ---------------------------------------------------------
		// 5) Controllo firing rate (con hysteresis + rate limiting)
		// ---------------------------------------------------------
		if (firingAll > firingHigh + config.FIRING_HYSTERESIS) {

			float dTh = clampDelta(0.01f, config.MAX_THRESHOLD_STEP);
			float dLeak = clampDelta(0.02f, config.MAX_LEAK_STEP);

			ncfg.FIRING_THRESHOLD += dTh;
			ncfg.REPOLARIZATION_PER_SECOND += dLeak;

			pcfg.A_PLUS  *= (1f - config.MAX_A_PLUS_FACTOR);
			pcfg.A_MINUS *= (1f + config.MAX_A_MINUS_FACTOR);

		} else if (lid != 0 && firingAll < firingLow - config.FIRING_HYSTERESIS) {
			// No firing control for sensorial layer 0 
			float dTh = (lid == 0 ? clampDelta(-0.005f, config.MAX_THRESHOLD_STEP)
                   : clampDelta(-0.01f, config.MAX_THRESHOLD_STEP));

			float dLeak = (lid == 0 ? clampDelta(-0.01f, config.MAX_LEAK_STEP)
                   : clampDelta(-0.02f, config.MAX_LEAK_STEP));

			ncfg.FIRING_THRESHOLD += dTh;
			ncfg.REPOLARIZATION_PER_SECOND += dLeak;

			pcfg.A_PLUS  *= (1f + config.MAX_A_PLUS_FACTOR);
			pcfg.A_MINUS *= (1f - config.MAX_A_MINUS_FACTOR);
		}

		// ---------------------------------------------------------
		// 6) Controllo sparsità
		// ---------------------------------------------------------
		if (sparsity < sparsityMin) {
			float dLeak = clampDelta(0.02f, config.MAX_LEAK_STEP);
			ncfg.REPOLARIZATION_PER_SECOND += dLeak;
		}
		else if (sparsity > sparsityMax) {
			float dLeak = clampDelta(-0.02f, config.MAX_LEAK_STEP);
			ncfg.REPOLARIZATION_PER_SECOND += dLeak;
		}

		// ---------------------------------------------------------
		// 7) Saturazione sinaptica
		// ---------------------------------------------------------
		if (satMaxRatio > config.MAX_SAT_MAX_RATIO) {
			pcfg.A_PLUS  *= (1f - config.MAX_A_PLUS_FACTOR);
			pcfg.A_MINUS *= (1f + config.MAX_A_MINUS_FACTOR);
			pcfg.HOMEOSTATIC_RATE *= 1.05f;
		}

		if (satMinRatio > config.MAX_SAT_MIN_RATIO) {
			pcfg.A_PLUS  *= (1f + config.MAX_A_PLUS_FACTOR);
			pcfg.A_MINUS *= (1f - config.MAX_A_MINUS_FACTOR);
			pcfg.HOMEOSTATIC_RATE *= 0.95f;
		}

		// ---------------------------------------------------------
		// 8) Energia
		// ---------------------------------------------------------
		if (energy > maxEnergy) {
			float dTh = clampDelta(0.01f, config.MAX_THRESHOLD_STEP);
			float dLeak = clampDelta(0.02f, config.MAX_LEAK_STEP);
			ncfg.FIRING_THRESHOLD += dTh;
			ncfg.REPOLARIZATION_PER_SECOND += dLeak;
		}

		// ---------------------------------------------------------
		// 9) Stabilità attivazione
		// ---------------------------------------------------------
		if (stability < config.MIN_STABILITY && firingAll > firingLow) {
			float dTh = clampDelta(0.01f, config.MAX_THRESHOLD_STEP);
			ncfg.FIRING_THRESHOLD += dTh;
		}

		// ---------------------------------------------------------
		// 10) Plasticità
		// ---------------------------------------------------------
		if (plasticity > config.MAX_PLASTICITY) {
			pcfg.ELIGIBILITY_DECAY *= 1.05f;
			pcfg.TAU_MINUS *= 1.05f;
		}

		if (plasticity < config.MIN_PLASTICITY) {
			pcfg.ELIGIBILITY_DECAY *= 0.95f;
			pcfg.TAU_MINUS *= 0.95f;
		}

		clampNeuronParams(ncfg);
		clampPlasticityParams(pcfg);
		
		// If a layer is dead..kick it!
		if (stats.activeNeurons() == 0 && lid == 0) {
			   // revival più deciso
		    ncfg.FIRING_THRESHOLD -= 0.05f;
		    ncfg.REPOLARIZATION_PER_SECOND -= 0.05f;

		    // clamp per evitare runaway
		    ncfg.FIRING_THRESHOLD = Math.max(0.1f, ncfg.FIRING_THRESHOLD);
		    ncfg.REPOLARIZATION_PER_SECOND = Math.max(0.01f, ncfg.REPOLARIZATION_PER_SECOND);
		}
		
		if (stats.activeNeurons() == 0 && lid > 0) {
		    ncfg.FIRING_THRESHOLD -= 0.02f;
		    ncfg.REPOLARIZATION_PER_SECOND -= 0.02f;

		    ncfg.FIRING_THRESHOLD = Math.max(0.15f, ncfg.FIRING_THRESHOLD);
		    ncfg.REPOLARIZATION_PER_SECOND = Math.max(0.02f, ncfg.REPOLARIZATION_PER_SECOND);
		}
	}

	private final static float clampDelta(float delta, float maxStep) {
		if (delta > maxStep) return maxStep;
		if (delta < -maxStep) return -maxStep;
		return delta;
	}

	private final static void clampNeuronParams(CorticalNeuronsConfig ncfg) {
		ncfg.FIRING_THRESHOLD =
				Math.max(0.1f, Math.min(ncfg.FIRING_THRESHOLD, 2.0f));

		ncfg.REPOLARIZATION_PER_SECOND =
				Math.max(0.01f, Math.min(ncfg.REPOLARIZATION_PER_SECOND, 1.0f));
	}

	private final static void clampPlasticityParams(ExcitatorySynapticPlasticityConfig pcfg) {
		pcfg.A_PLUS =
				Math.max(0.00001f, Math.min(pcfg.A_PLUS, 0.01f));

		pcfg.A_MINUS =
				Math.max(0.00001f, Math.min(pcfg.A_MINUS, 0.01f));
	}

	private void log(Layer layer, String msg) {
		// puoi sostituire con logger
		// System.out.println("[Stabilizer][L" + layer.getLayerId() + "] " + msg);
	}
}