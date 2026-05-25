package com.cortex.globals;

import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.brain.Brain;
import com.cortex.brain.layers.Layer;
import com.cortex.globals.DiscreteAdaptiveStabilizerConfig.LayerAdaptiveParams;
import com.cortex.globals.EventBus.EventListener;
import com.cortex.globals.EventBus.EventType;
import com.cortex.metrics.LayerStats;

public class DiscreteAdaptiveStabilizer {

	private final Brain brain;
	private final DiscreteAdaptiveStabilizerConfig config;
	private final LayerStats[] layerStats;
	private final StatsEMA[] ema;

	private int ticksSinceLastIntervention = 0;

	private static class StatsEMA {
		float fireAll = 0;
		float sparsity = 0;
		float satMax = 0;
		float satMin = 0;
		float energy = 0;
		float plast = 0;
		float stability = 0;
	}

	private void updateEMA(LayerStats s) {
		float a = 0.2f; // smoothing
		int lid = s.layer().getLayerId();
		if (ema[lid]==null) {
			ema[lid] = new StatsEMA();
		}
		
		double effectiveSparsity = 1.0 - (s.activeNeurons() / (double) s.layer().getNeuronsCount());
		
		ema[lid].fireAll = (float)( a * s.avgFiringRateAll() + (1-a) * ema[lid].fireAll);
		ema[lid].sparsity =(float)( a * ema[lid].sparsity  + effectiveSparsity * (1 - a));
		ema[lid].satMax = (float)(a * s.saturatedMaxRatio() + (1-a) * ema[lid].satMax);
		ema[lid].satMin = (float)(a * s.saturatedMinRatio() + (1-a) * ema[lid].satMin);
		ema[lid].energy = (float)(a * s.energy() + (1-a) * ema[lid].energy);
		ema[lid].plast = (float)(a * s.totalPlasticity() + (1-a) * ema[lid].plast);
		ema[lid].stability = (float)(a * s.activationStability() + (1-a) * ema[lid].stability);
	}

	public DiscreteAdaptiveStabilizer( Brain brain, DiscreteAdaptiveStabilizerConfig config ) {
		this.config = config;
		this.brain = brain;
		this.layerStats = new LayerStats[ brain.getAllLayers().size() ];
		this.ema = new StatsEMA[brain.getAllLayers().size()];

		EventBus.addListener(EventType.LAYER_STATS,  new EventListener() {

			@Override
			public void onEvent(EventType type, long time, Object source, Object data) {
				LayerStats stats = (LayerStats)data;
				if (stats!=null) {
					updateEMA( stats );
					layerStats[stats.layer().getLayerId()] = stats;
					stabilize( stats );
				}
			}
		});
	}

	public void stabilize(LayerStats stats) {
		stabilizeLocal(stats);

		for (int lid = 0; lid < brain.getAllLayers().size()-1; lid++) {
			LayerStats src = layerStats[lid];
			LayerStats dst = layerStats[lid+1];
			if (src!=null && dst!=null) {
				stabilizePropagation(src, dst);
			}
		}

		stabilizeGlobal();
	}

	private void stabilizeGlobal() {
		float avgFire = 0f;
		int layers = brain.getAllLayers().size();

		for (int lid = 0; lid < brain.getAllLayers().size()-1; lid++) {
			if (layerStats[lid]!=null && layerStats[lid].activeNeurons() > 0) {
				avgFire += ema[lid].fireAll;
			}
		}
		avgFire /= layers;

		if (avgFire < 0.005f) {
			// dead
			boostGlobalExcitation();
		} else {
			if (avgFire > 0.15f) {
				dampGlobalExcitation();
			} else if (avgFire < 0.02f) {
				boostGlobalExcitation();
			}
		}
	}	

	private void boostGlobalExcitation() {
		for (Layer l : brain.getAllLayers()) {
			LayerConfig lc = l.getConfig();
			ExcitatorySynapticPlasticityConfig e = lc.SYNAPSE_PLASTICITY_CONFIG.excitatory();
			e.INITIAL_WEIGHT *= 1.03f;
			e.W_BASELINE *= 1.03f;
			e.HOMEOSTATIC_RATE *= 0.95f;
			clampWeights(e);
		}
	}

	private void dampGlobalExcitation() {
		for (Layer l : brain.getAllLayers()) {
			LayerConfig lc = l.getConfig();
			ExcitatorySynapticPlasticityConfig e = lc.SYNAPSE_PLASTICITY_CONFIG.excitatory();
			e.INITIAL_WEIGHT *= 0.97f;
			e.W_BASELINE *= 0.97f;
			e.HOMEOSTATIC_RATE *= 1.05f;
			clampWeights(e);
		}
	}

	private void stabilizePropagation(LayerStats src, LayerStats dst) {
		ExcitatorySynapticPlasticityConfig e 
			= src.layer().getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatory();

		// Se il layer successivo è morto → aumentare drive feedforward
		if (dst.activeNeurons() == 0) {
			e.INITIAL_WEIGHT += 0.1f;
			e.W_BASELINE += 0.1f;
			e.HOMEOSTATIC_RATE *= 0.90f;
			clampWeights(e);
			return;
		}
		
		StatsEMA srcEma = ema[src.layer().getLayerId()];
		StatsEMA dstEma = ema[dst.layer().getLayerId()];
	    float srcF = srcEma.fireAll;
	    float dstF = dstEma.fireAll;
	    float ratio = dstF / (srcF + 1e-6f);
	    
	    // Propagazione troppo debole
	    if (ratio < 0.15f && srcF > 0.02f) {
	        e.INITIAL_WEIGHT += 0.05f;
	        e.W_BASELINE    += 0.05f;
	    }

	    // Propagazione troppo forte
	    if (ratio > 0.70f && dstF > 0.05f) {
	        e.INITIAL_WEIGHT -= 0.03f;
	        e.W_BASELINE    -= 0.03f;
	    }

	    clampWeights(e);
		
	}

	private void stabilizeLocal(LayerStats stats) {
	    ticksSinceLastIntervention++;
	    if (ticksSinceLastIntervention < 3) return;
	    ticksSinceLastIntervention = 0;

	    Layer layer = stats.layer();
	    int lid = layer.getLayerId();

	    CorticalNeuronsConfig ncfg =
	            layer.getConfig().CORTICAL_NEURONS_CONFIG;

	    ExcitatorySynapticPlasticityConfig pcfg =
	            layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatory();

	    // ---------------------------------------------------------
	    // 1) Recupero / inizializzo parametri adattivi per-layer
	    // ---------------------------------------------------------
	    DiscreteAdaptiveStabilizerConfig.LayerAdaptiveParams lp =
	            config.perLayer.get(lid);

	    if (lp == null) {
	        lp = new DiscreteAdaptiveStabilizerConfig.LayerAdaptiveParams();
	        lp.TARGET_FIRING_LOW   = config.TARGET_FIRING_LOW;
	        lp.TARGET_FIRING_HIGH  = config.TARGET_FIRING_HIGH;
	        lp.TARGET_SPARSITY_MIN = config.TARGET_SPARSITY_MIN;
	        lp.TARGET_SPARSITY_MAX = config.TARGET_SPARSITY_MAX;
	        lp.MAX_ENERGY          = config.MAX_ENERGY_PER_LAYER;
	        config.perLayer.put(lid, lp);
	    }

	    // piccoli learning rate per auto‑taratura dei target
	    final float LR_FIRING   = 0.01f;
	    final float LR_SPARSITY = 0.01f;

	    float firingLow   = lp.TARGET_FIRING_LOW;
	    float firingHigh  = lp.TARGET_FIRING_HIGH;
	    float sparsityMin = lp.TARGET_SPARSITY_MIN;
	    float sparsityMax = lp.TARGET_SPARSITY_MAX;
	    float maxEnergy   = lp.MAX_ENERGY;

	    // ---------------------------------------------------------
	    // 2) Lettura metriche (EMA)
	    // ---------------------------------------------------------
	    double firingAll   = ema[lid].fireAll;
	    double sparsity    = ema[lid].sparsity;
	    double satMaxRatio = ema[lid].satMax;
	    double satMinRatio = ema[lid].satMin;
	    double energy      = ema[lid].energy;
	    double stability   = ema[lid].stability;
	    double plasticity  = ema[lid].plast;

	    // ---------------------------------------------------------
	    // 3) Auto‑taratura dei target in base a ciò che si osserva
	    // ---------------------------------------------------------
	    // Se il layer è quasi sempre morto, abbassa un po' i target di firing
	    if (firingAll < 0.01) {
	        lp.TARGET_FIRING_LOW  = (1 - LR_FIRING) * lp.TARGET_FIRING_LOW
	                              + LR_FIRING * (float)(firingAll * 0.5);
	        lp.TARGET_FIRING_HIGH = (1 - LR_FIRING) * lp.TARGET_FIRING_HIGH
	                              + LR_FIRING * (float)(firingAll * 1.5);
	    } else {
	        // altrimenti fai convergere i target verso il firing osservato
	        float center = (float)firingAll;
	        float span   = Math.max(0.02f, center * 0.5f); // ampiezza banda
	        lp.TARGET_FIRING_LOW  = (1 - LR_FIRING) * lp.TARGET_FIRING_LOW
	                              + LR_FIRING * (center - span);
	        lp.TARGET_FIRING_HIGH = (1 - LR_FIRING) * lp.TARGET_FIRING_HIGH
	                              + LR_FIRING * (center + span);
	    }

	    // Sparsità: fai convergere la finestra verso la sparsità osservata
	    if (sparsity > 0.0 && sparsity < 1.0) {
	        float sCenter = (float)sparsity;
	        float sSpan   = 0.1f; // banda di sparsità desiderata
	        float newMin  = Math.max(0.0f, sCenter - sSpan);
	        float newMax  = Math.min(1.0f, sCenter + sSpan);

	        lp.TARGET_SPARSITY_MIN = (1 - LR_SPARSITY) * lp.TARGET_SPARSITY_MIN
	                               + LR_SPARSITY * newMin;
	        lp.TARGET_SPARSITY_MAX = (1 - LR_SPARSITY) * lp.TARGET_SPARSITY_MAX
	                               + LR_SPARSITY * newMax;
	    }

	    // rileggo i target aggiornati
	    firingLow   = lp.TARGET_FIRING_LOW;
	    firingHigh  = lp.TARGET_FIRING_HIGH;
	    sparsityMin = lp.TARGET_SPARSITY_MIN;
	    sparsityMax = lp.TARGET_SPARSITY_MAX;
	    maxEnergy   = lp.MAX_ENERGY;

	    // ---------------------------------------------------------
	    // 4) Vincoli "morbidi" per layer (solo limiti, non set fissi)
	    // ---------------------------------------------------------
	    if (lid == 0) ncfg.REPOLARIZATION_PER_SECOND =
	            Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.15f);
	    if (lid == 1) ncfg.REPOLARIZATION_PER_SECOND =
	            Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.15f);
	    if (lid == 2) ncfg.REPOLARIZATION_PER_SECOND =
	            Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.20f);
	    if (lid == 3) ncfg.REPOLARIZATION_PER_SECOND =
	            Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.15f);
	    if (lid == 4) ncfg.REPOLARIZATION_PER_SECOND =
	            Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.12f);
	    if (lid == 5) ncfg.REPOLARIZATION_PER_SECOND =
	            Math.min(ncfg.REPOLARIZATION_PER_SECOND, 0.10f);

	    if (lid == 3) ncfg.FIRING_THRESHOLD =
	            Math.max(ncfg.FIRING_THRESHOLD, 0.30f);
	    if (lid == 4) ncfg.FIRING_THRESHOLD =
	            Math.max(ncfg.FIRING_THRESHOLD, 0.35f);
	    if (lid == 5) ncfg.FIRING_THRESHOLD =
	            Math.max(ncfg.FIRING_THRESHOLD, 0.40f);

	    // ---------------------------------------------------------
	    // 5) Controllo firing rate (adattivo, scalato sull'errore)
	    // ---------------------------------------------------------
	    float centerTarget = 0.5f * (firingLow + firingHigh);
	    float spanTarget   = Math.max(1e-4f, firingHigh - firingLow);

	    float errF = (float)firingAll - centerTarget;
	    float normErrF = errF / spanTarget; // errore normalizzato

	    if (Math.abs(normErrF) > config.FIRING_HYSTERESIS) {
	        float sign = Math.signum(normErrF);
	        // più sei lontano, più spingi
	        float scale = Math.min(1.0f, Math.abs(normErrF));

	        float dTh   = clampDelta(0.01f * sign * scale, config.MAX_THRESHOLD_STEP);
	        float dLeak = clampDelta(0.02f * sign * scale, config.MAX_LEAK_STEP);

	        ncfg.FIRING_THRESHOLD          += dTh;
	        ncfg.REPOLARIZATION_PER_SECOND += dLeak;

	        if (sign > 0) {
	            // firing troppo alto → riduci LTP, aumenta LTD
	            pcfg.A_PLUS  *= (1f - config.MAX_A_PLUS_FACTOR * scale);
	            pcfg.A_MINUS *= (1f + config.MAX_A_MINUS_FACTOR * scale);
	        } else {
	            // firing troppo basso → aumenta LTP, riduci LTD
	            pcfg.A_PLUS  *= (1f + config.MAX_A_PLUS_FACTOR * scale);
	            pcfg.A_MINUS *= (1f - config.MAX_A_MINUS_FACTOR * scale);
	        }
	    }

	    // ---------------------------------------------------------
	    // 6) Controllo sparsità (adattivo)
	    // ---------------------------------------------------------
	    if (sparsity < sparsityMin || sparsity > sparsityMax) {
	        float sCenter = 0.5f * (sparsityMin + sparsityMax);
	        float sSpan   = Math.max(1e-4f, sparsityMax - sparsityMin);
	        float errS    = (float)sparsity - sCenter;
	        float normErrS = errS / sSpan;

	        float signS  = Math.signum(normErrS);
	        float scaleS = Math.min(1.0f, Math.abs(normErrS));

	        // se sparsità troppo bassa → aumenti leak (meno attivi)
	        // se troppo alta → riduci leak (più attivi)
	        float dLeak = clampDelta(0.02f * signS * scaleS, config.MAX_LEAK_STEP);
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
	        float dTh   = clampDelta(0.01f, config.MAX_THRESHOLD_STEP);
	        float dLeak = clampDelta(0.02f, config.MAX_LEAK_STEP);
	        ncfg.FIRING_THRESHOLD          += dTh;
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
	        pcfg.TAU_MINUS         *= 1.05f;
	    }

	    if (plasticity < config.MIN_PLASTICITY) {
	        pcfg.ELIGIBILITY_DECAY *= 0.95f;
	        pcfg.TAU_MINUS         *= 0.95f;
	    }

	    float ratio = pcfg.A_PLUS / pcfg.A_MINUS;
	    if (ratio < 0.5f) pcfg.A_PLUS  *= 1.05f;
	    if (ratio > 2.0f) pcfg.A_MINUS *= 1.05f;

	    clampNeuronParams(ncfg);
	    clampPlasticityParams(pcfg);

	    // ---------------------------------------------------------
	    // 11) Layer morto → kick, ma con target che si sono già abbassati
	    // ---------------------------------------------------------
	    if (stats.activeNeurons() == 0) {
	        ncfg.FIRING_THRESHOLD          -= 0.02f;
	        ncfg.REPOLARIZATION_PER_SECOND -= 0.02f;

	        pcfg.INITIAL_WEIGHT    += 0.02f;
	        pcfg.W_BASELINE        += 0.02f;
	        pcfg.HOMEOSTATIC_RATE  *= 0.95f;

	        clampWeights(pcfg);

	        ncfg.FIRING_THRESHOLD          = Math.max(0.15f, ncfg.FIRING_THRESHOLD);
	        ncfg.REPOLARIZATION_PER_SECOND = Math.max(0.02f,  ncfg.REPOLARIZATION_PER_SECOND);
	    }
	}



	private final static float clampDelta(float delta, float maxStep) {
		if (delta > maxStep) return maxStep;
		if (delta < -maxStep) return -maxStep;
		return delta;
	}

	private final static float clamp(float val, float min, float max) {
		return Math.max(min, Math.min(val, max));
	}

	private final static void clampNeuronParams(CorticalNeuronsConfig ncfg) {
		ncfg.FIRING_THRESHOLD = clamp(ncfg.FIRING_THRESHOLD, 0.1f, 2.0f );
		ncfg.REPOLARIZATION_PER_SECOND = clamp(ncfg.REPOLARIZATION_PER_SECOND, 0.02f, 1.0f );
	}

	private final static void clampPlasticityParams(ExcitatorySynapticPlasticityConfig pcfg) {
		pcfg.A_PLUS = clamp(pcfg.A_PLUS, 0.00001f, 0.01f);
		pcfg.A_MINUS = clamp(pcfg.A_MINUS, 0.00001f, 0.01f);	
		pcfg.HOMEOSTATIC_RATE = clamp(pcfg.HOMEOSTATIC_RATE, 0.0001f, 0.01f);
		pcfg.W_MAX = clamp(pcfg.W_MAX, 0.20f, 1.0f);
	}

	private static void clampWeights(ExcitatorySynapticPlasticityConfig e) {
		e.INITIAL_WEIGHT = clamp(e.INITIAL_WEIGHT, 0.05f, 0.60f);
		e.W_BASELINE     = clamp(e.W_BASELINE, 0.05f, 0.60f);
	}

	private void log(Layer layer, String msg) {
		// puoi sostituire con logger
		// System.out.println("[Stabilizer][L" + layer.getLayerId() + "] " + msg);
	}
}