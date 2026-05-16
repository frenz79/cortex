package com.cortex.base;

import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;

/**
 Design goals (chiari)

La plasticità deve poter modellare:
	STDP temporale (core)
	Eligibility trace (learning differito)
	Modulazione globale (reward / punishment)
	Homeostasi (evitare runaway)
	Decay lento verso baseline
	Plasticità opzionale del delay
	Cap hard e soft
	Switch on/off per tipo di sinapsi

Concetto chiave: Eligibility Trace
	“Una sinapsi diventa eleggibile al cambiamento, ma viene modificata solo quando arriva un segnale globale.”
 
 
 public void applyReward(float reward, long now, float neuromodulator) {
    if (!enabled) return;
    weight += reward * eligibility * neuromodulator;
    weight = clamp(weight, W_MIN, W_MAX);
    eligibility = 0f;
}

 */
public final class ExcitatorySynapticPlasticity implements IPlasticityRule {

	public static record ExcitatorySynapticPlasticityConfig (
			// STDP
			float A_PLUS,				// 0.01f
			float A_MINUS,				// 0.012
			long TAU_PLUS,				// 20
			long TAU_MINUS,			// 20
			// WEIGHTS
			float W_MIN,				// 0.0f
			float W_MAX,				// 1.0f
			float W_BASELINE,			// 0.2f
			// ELIGIBILITY TRACE
			float ELIGIBILITY_DECAY,	// 0.95f
			// HOMEOSTASIS
			float HOMEOSTATIC_RATE,		// 0.0005f
			// DELAY PLASTICITY
			boolean PLASTIC_DELAY,		// false
			float DELAY_MIN,			// 1f
			float DELAY_MAX				// 20f
		) {
			public ExcitatorySynapticPlasticityConfig with_W_BASELINE(float newW_BASELINE) {
				return new ExcitatorySynapticPlasticityConfig(
						A_PLUS,				// 0.01f
						A_MINUS,				// 0.012
						 TAU_PLUS,				// 20
						 TAU_MINUS,			// 20
						// WEIGHTS
						 W_MIN,				// 0.0f
						 W_MAX,				// 1.0f
						 newW_BASELINE,			// 0.2f
						// ELIGIBILITY TRACE
						 ELIGIBILITY_DECAY,	// 0.95f
						// HOMEOSTASIS
						 HOMEOSTATIC_RATE,		// 0.0005f
						// DELAY PLASTICITY
						 PLASTIC_DELAY,		// false
						 DELAY_MIN,			// 1f
						 DELAY_MAX				// 20f	
				);
			}
	}
	
	private final ExcitatorySynapticPlasticityConfig config;
	
    private float eligibility = 0.0f;
    private long lastEligibilityUpdate = 0l;
    private float delay;
    private float weight;
    private long lastPreSpike = -1;
    private long lastPostSpike = -1;
    private boolean enabled = true;

    public ExcitatorySynapticPlasticity(float initialWeight, float initialDelay, ExcitatorySynapticPlasticityConfig config) {
        this.weight = initialWeight;
        this.delay = initialDelay;
        this.config = config;
    }


    // IPlasticityRule
    @Override
    public boolean onPreSpike(long time) {
        lastPreSpike = time;
        if (lastPostSpike >= 0) {
            long dt = lastPostSpike - time;
            return updateEligibility(dt);
        }
        return false;
    }
 // IPlasticityRule
    @Override
    public boolean onPostSpike(Synapse s, long time, long now) {
        lastPostSpike = time;
        if (lastPreSpike >= 0) {
            long dt = time - lastPreSpike;
            return updateEligibility(dt);
        }
        return false;
    }

    // =========================================================
    // CORE STDP (eligibility, non peso diretto!)
    // =========================================================

    private boolean updateEligibility(long dt) {
        float delta;
        if (dt > 0) {
            delta = config.A_PLUS * (float)Math.exp(-dt / config.TAU_PLUS);
        } else {
            delta = -config.A_MINUS * (float)Math.exp(dt / config.TAU_MINUS);
        }
        eligibility += delta;
        eligibility = Maths.clamp(eligibility, -1f, 1f);
        
       return (delta != 0f);
    }

    // =========================================================
    // REWARD / PUNISHMENT
    // =========================================================

    public void applyReward(float reward, long now) {
        if (!enabled) return;
        weight += reward * eligibility;
        weight = Maths.clamp(weight, config.W_MIN, config.W_MAX);
        eligibility = 0f;
    }
    
    @Override
    public boolean isEligible(long now, long window) {
        return eligibility != 0f && (now - lastEligibilityUpdate) <= window;
    }
    
    public void onEligibilityUpdate(float delta, long now) {
        eligibility += delta;
        lastEligibilityUpdate = now;
    }
    
    // =========================================================
    // HOMEOSTASI + DECAY
    // =========================================================

    public void update(long now) {
        // decay eligibility
        eligibility *= config.ELIGIBILITY_DECAY;

        // homeostasi verso baseline
        weight += config.HOMEOSTATIC_RATE * (config.W_BASELINE - weight);
        weight = Maths.clamp(weight, config.W_MIN, config.W_MAX);
    }

    // =========================================================
    // DELAY PLASTICITY (opzionale)
    // =========================================================

    public void updateDelay(float reward) {
        if (!config.PLASTIC_DELAY) return;
        delay += reward * eligibility * 0.1f;
        delay = Maths.clamp(delay, config.DELAY_MIN, config.DELAY_MAX);
    }

    // =========================================================
    // ACCESSORS
    // =========================================================

    public float getWeight() {
        return weight;
    }

    public float getDelay() {
        return delay;
    }
  
}

