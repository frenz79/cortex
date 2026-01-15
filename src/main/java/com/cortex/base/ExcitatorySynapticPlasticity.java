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

    // ==== PARAMETRI STDP ====
    private static final float A_PLUS = 0.01f;
    private static final float A_MINUS = 0.012f;
    private static final float TAU_PLUS = 20;
    private static final float TAU_MINUS = 20;

    // ==== PESI ====
    private static final float W_MIN = 0.0f;
    private static final float W_MAX = 1.0f;
    private static final float W_BASELINE = 0.2f;

    // ==== ELIGIBILITY TRACE ====
    private static final float ELIGIBILITY_DECAY = 0.95f;
    private float eligibility = 0.0f;

    // ==== HOMEOSTASI ====
    private static final float HOMEOSTATIC_RATE = 0.0005f;

    // ==== DELAY PLASTICITY ====
    private boolean plasticDelay = false;
    private float delay;
    private static final float DELAY_MIN = 1f;
    private static final float DELAY_MAX = 20f;

    // ==== STATO ====
    private float weight;
    private long lastPreSpike = -1;
    private long lastPostSpike = -1;

    // ==== ENABLE FLAGS ====
    private boolean enabled = true;

    public ExcitatorySynapticPlasticity(float initialWeight, float initialDelay) {
        this.weight = initialWeight;
        this.delay = initialDelay;
    }


    // IPlasticityRule
    @Override
    public void onPreSpike(long time) {
        lastPreSpike = time;
        if (lastPostSpike >= 0) {
            long dt = lastPostSpike - time;
            updateEligibility(dt);
        }
    }
 // IPlasticityRule
    @Override
    public void onPostSpike(Synapse s, long time, long now) {
        lastPostSpike = time;
        if (lastPreSpike >= 0) {
            long dt = time - lastPreSpike;
            updateEligibility(dt);
        }
    }

    // =========================================================
    // CORE STDP (eligibility, non peso diretto!)
    // =========================================================

    private void updateEligibility(long dt) {
        float delta;
        if (dt > 0) {
            delta = A_PLUS * (float)Math.exp(-dt / TAU_PLUS);
        } else {
            delta = -A_MINUS * (float)Math.exp(dt / TAU_MINUS);
        }
        eligibility += delta;
        eligibility = Maths.clamp(eligibility, -1f, 1f);
    }

    // =========================================================
    // REWARD / PUNISHMENT
    // =========================================================

    public void applyReward(float reward, long now) {
        if (!enabled) return;
        weight += reward * eligibility;
        weight = Maths.clamp(weight, W_MIN, W_MAX);
        eligibility = 0f;
    }

    // =========================================================
    // HOMEOSTASI + DECAY
    // =========================================================

    public void update(long now) {
        // decay eligibility
        eligibility *= ELIGIBILITY_DECAY;

        // homeostasi verso baseline
        weight += HOMEOSTATIC_RATE * (W_BASELINE - weight);
        weight = Maths.clamp(weight, W_MIN, W_MAX);
    }

    // =========================================================
    // DELAY PLASTICITY (opzionale)
    // =========================================================

    public void updateDelay(float reward) {
        if (!plasticDelay) return;
        delay += reward * eligibility * 0.1f;
        delay = Maths.clamp(delay, DELAY_MIN, DELAY_MAX);
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

    public void setPlasticDelay(boolean enabled) {
        this.plasticDelay = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

   
}

