package com.cortex.globals;

import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.brain.Brain;
import com.cortex.brain.layers.Layer;
import com.cortex.metrics.LayerStats;
import com.cortex.metrics.MetricsRecorder;

public class AdaptiveStabilizer {
	private final Brain brain;
    private final MetricsRecorder metrics;

    // Target “morbidi” per tutti i layer (puoi specializzarli per layer)
    private final double targetFiringLow   = 5.0;    // spike / step / neurone (ALL)
    private final double targetFiringHigh  = 20.0;

    private final double targetSparsityMin = 0.85;   // almeno 85% silenti
    private final double targetSparsityMax = 0.99;

    private final double maxSatMaxRatio    = 0.15;   // max 15% sinapsi a W_MAX
    private final double maxSatMinRatio    = 0.20;   // max 20% sinapsi a W_MIN

    private final double maxEnergyPerLayer = 120_000.0;

    public AdaptiveStabilizer(Brain brain, MetricsRecorder metrics) {
        this.brain = brain;
        this.metrics = metrics;
    }

    /**
     * Chiamare periodicamente (es. ogni 200–500 ms di tempo simulato o reale).
     */
    public void step() {
        for (Layer layer : brain.getAllLayers()) {
            int layerId = layer.getLayerId();
            LayerStats stats = metrics.pollLayerStats(layerId);
            if (stats == null) {
                continue;
            }
            adaptLayer(layer, stats);
        }
    }

    private void adaptLayer(Layer layer, LayerStats stats) {
        int id = layer.getLayerId();

        // Qui assumo che tu possa accedere a una config mutabile o a metodi di tuning.
        // Se oggi la config è immutabile, puoi far sì che il layer esponga metodi tipo:
        //   layer.adjustFiringThreshold(delta);
        //   layer.adjustRepolarization(delta);
        //   layer.scaleExcitatoryLearningRate(factor);
        // ecc.
        CorticalNeuronsConfig neuronsCfg =
                layer.getConfig().CORTICAL_NEURONS_CONFIG;
        ExcitatorySynapticPlasticityConfig excCfg =
                layer.getConfig().SYNAPSE_PLASTICITY_CONFIG.excitatorySynapticPlasticityConfig();

        double firingAll   = stats.avgFiringRateAll();
        double sparsity    = stats.sparsity();
        double satMaxRatio = stats.saturatedMaxRatio();
        double satMinRatio = stats.saturatedMinRatio();
        double energy      = stats.energy();
        double stability   = stats.activationStability();
        double plasticity  = stats.totalPlasticity();

        // ---------------------------------------------------------
        // 1) Controllo firing rate globale
        // ---------------------------------------------------------
        if (firingAll > targetFiringHigh) {
        	float delta = 0.01f;
            // Troppa attività → alza leggermente la soglia
        	neuronsCfg.FIRING_THRESHOLD += 0.01f;
        	neuronsCfg.REPOLARIZATION_PER_SECOND += 0.02f;
        	excCfg.A_PLUS *= 0.98f;
        	excCfg.A_MINUS *= 1.02f;
            log(id, "HIGH FIRING (" + firingAll + ") → +threshold " + delta);
        } else if (firingAll < targetFiringLow) {
        	float delta = 0.01f;
            // Troppo poco → abbassa leggermente la soglia
        	neuronsCfg.FIRING_THRESHOLD -= 0.01f;
        	neuronsCfg.REPOLARIZATION_PER_SECOND = Math.max(0.01f, neuronsCfg.REPOLARIZATION_PER_SECOND - 0.02f);
        	excCfg.A_PLUS *= 1.02f;
        	excCfg.A_MINUS *= 0.98f;
            neuronsCfg.FIRING_THRESHOLD -= delta;
            log(id, "LOW FIRING (" + firingAll + ") → -threshold " + delta);
        }

        // ---------------------------------------------------------
        // 2) Controllo sparsità
        // ---------------------------------------------------------
        if (sparsity < targetSparsityMin) {
            // Troppi neuroni attivi → aumenta leak / repolarizzazione
            float delta = 0.02f;
            neuronsCfg.REPOLARIZATION_PER_SECOND += delta;
            log(id, "LOW SPARSITY (" + sparsity + ") → +repolarization " + delta);
        } else if (sparsity > targetSparsityMax) {
            // Troppo pochi neuroni attivi → riduci leak
            float delta = 0.02f;
            neuronsCfg.REPOLARIZATION_PER_SECOND -= delta;
            log(id, "HIGH SPARSITY (" + sparsity + ") → -repolarization " + delta);
        }

        // ---------------------------------------------------------
        // 3) Controllo saturazione sinaptica
        // ---------------------------------------------------------
        if (satMaxRatio > maxSatMaxRatio) {
        	float factor = 0.97f;
            // Troppe sinapsi a W_MAX → riduci leggermente il learning rate
        	excCfg.A_PLUS *= 0.97f;
        	excCfg.A_MINUS *= 1.03f;
        	excCfg.HOMEOSTATIC_RATE *= 1.05f;
            log(id, "HIGH SAT_MAX (" + satMaxRatio + ") → LR * " + factor);
        }
        if (satMinRatio > maxSatMinRatio) {
        	float factor = 1.03f;
            // Troppe sinapsi a W_MIN → aumenta leggermente il learning rate
        	excCfg.A_PLUS *= 1.03f;
        	excCfg.A_MINUS *= 0.97f;
        	excCfg.HOMEOSTATIC_RATE *= 0.95f;
            log(id, "HIGH SAT_MIN (" + satMinRatio + ") → LR * " + factor);
        }

        // ---------------------------------------------------------
        // 4) Controllo energia
        // ---------------------------------------------------------
        if (energy > maxEnergyPerLayer) {
            // Layer troppo “energetico” → aumenta threshold e leak
            float dTh   = 0.01f;
            float dLeak = 0.02f;            
            neuronsCfg.FIRING_THRESHOLD += dTh;
            neuronsCfg.REPOLARIZATION_PER_SECOND += dLeak;
            log(id, "HIGH ENERGY (" + energy + ") → +threshold " + dTh + ", +repolarization " + dLeak);
        }

        // ---------------------------------------------------------
        // 5) Controllo stabilità attivazione
        // ---------------------------------------------------------
        if (stability < 0.15 && firingAll > targetFiringLow) {
            // Pattern che cambiano troppo → layer instabile → alza threshold
            float dTh = 0.01f;
            neuronsCfg.FIRING_THRESHOLD = (neuronsCfg.FIRING_THRESHOLD + dTh);
            log(id, "LOW STABILITY (" + stability + ") → +threshold " + dTh);
        }

        // ---------------------------------------------------------
        // 6) Controllo plasticità eccessiva
        // ---------------------------------------------------------
        double plastHigh = 10_000.0; // da calibrare
        if (plasticity > plastHigh) {
        	float factor = 1.05f;
            excCfg.ELIGIBILITY_DECAY *= 1.05f;
            excCfg.TAU_MINUS *= 1.05f;
            log(id, "HIGH PLASTICITY (" + plasticity + ") → LR * " + factor);
        }
        // ---------------------------------------------------------
        // 7) plasticità bassa
        // ---------------------------------------------------------
        if (plasticity < 50.0) {
        	float factor = 0.95f;
        	excCfg.ELIGIBILITY_DECAY *= 0.95f;
        	excCfg.TAU_MINUS *= 0.95f;
        	log(id, "LOW PLASTICITY (" + plasticity + ") → LR * " + factor);
        }
    }

    private void log(int layerId, String msg) {
        // puoi sostituire con logger
        System.out.println("[Stabilizer][L" + layerId + "] " + msg);
    }
}