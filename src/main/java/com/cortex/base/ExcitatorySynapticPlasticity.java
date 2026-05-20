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
		 public ExcitatorySynapticPlasticityConfig {
	            if (W_MIN < 0f || W_MAX <= W_MIN) throw new IllegalArgumentException("Invalid weight bounds");
	            if (TAU_PLUS <= 0L || TAU_MINUS <= 0L) throw new IllegalArgumentException("TAU must be > 0 (nanos)");
	            if (ELIGIBILITY_DECAY <= 0f || ELIGIBILITY_DECAY > 1f) throw new IllegalArgumentException("ELIGIBILITY_DECAY must be in (0,1]");
	        }
		 
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
	private long lastEligibilityUpdate = -1l;
	private float delay;
	private float weight;
	private long lastPreSpike = -1l;
	private long lastPostSpike = -1l;
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
        if (lastPostSpike >= 0L) {
            long dt = lastPostSpike - time; // post - pre
            float delta = computeStdpDelta(dt);
            if (delta != 0f) {
                onEligibilityUpdate(delta, time);
                return true;
            }
        }
        return false;
	}
	// IPlasticityRule
	@Override
	public boolean onPostSpike(Synapse s, long time, long now) {
		  lastPostSpike = time;
	        if (lastPreSpike >= 0L) {
	            long dt = time - lastPreSpike; // post - pre
	            float delta = computeStdpDelta(dt);
	            if (delta != 0f) {
	                onEligibilityUpdate(delta, now);
	                return true;
	            }
	        }
	        return false;
	}
	
	 // compute STDP delta given dt = postTime - preTime (nanos)
    private float computeStdpDelta(long dtNanos) {
        // convert to double to avoid integer division
        double dt = (double) dtNanos;
        if (dt > 0.0) {
            return (float) (config.A_PLUS * Math.exp(-dt / (double) config.TAU_PLUS));
        } else {
            // dt <= 0 : depression
            return (float) (-config.A_MINUS * Math.exp(dt / (double) config.TAU_MINUS));
        }
    }

	// =========================================================
	// REWARD / PUNISHMENT
	// =========================================================

	public void applyReward(float reward, long now, float neuromodulator) {
        if (!enabled) return;
        if (eligibility == 0f) return;
        float deltaW = reward * eligibility * neuromodulator;
        weight += deltaW;
        weight = Maths.clamp(weight, config.W_MIN, config.W_MAX);
        // consume eligibility
        eligibility = 0f;
        lastEligibilityUpdate = now;
	}

    @Override
    public boolean isEligible(long now, long window) {
        if (lastEligibilityUpdate <= 0L) return false;
        return eligibility != 0f && (now - lastEligibilityUpdate) <= window;
    }

	public void onEligibilityUpdate(float delta, long now) {
	     eligibility += delta;
	        eligibility = Maths.clamp(eligibility, -1f, 1f);
	        lastEligibilityUpdate = now;
	}

	// =========================================================
	// HOMEOSTASI + DECAY
	// =========================================================

	 // update called periodically; compute time-based decay for eligibility and homeostasis
    public void update(long now) {
        if (lastEligibilityUpdate > 0L) {
            long dt = now - lastEligibilityUpdate; // nanos
            // convert to seconds for decay exponent if ELIGIBILITY_DECAY is per-second factor
            double seconds = dt / 1_000_000_000.0;
            // decayFactor = ELIGIBILITY_DECAY ^ seconds
            double decayFactor = Math.pow(config.ELIGIBILITY_DECAY, seconds);
            eligibility *= (float) decayFactor;
            // if very small, zero it
            if (Math.abs(eligibility) < 1e-6f) eligibility = 0f;
            lastEligibilityUpdate = now;
        }

        // homeostatic drift towards baseline (time-independent small step)
        weight += config.HOMEOSTATIC_RATE * (config.W_BASELINE - weight);
        weight = Maths.clamp(weight, config.W_MIN, config.W_MAX);
    }
    
    public void updateDelay(float reward) {
        if (!config.PLASTIC_DELAY) return;
        delay += reward * eligibility * 0.1f;
        delay = Maths.clamp(delay, config.DELAY_MIN, config.DELAY_MAX);
    }

	public float getWeight() {
		return weight;
	}

	public float getDelay() {
		return delay;
	}

}

