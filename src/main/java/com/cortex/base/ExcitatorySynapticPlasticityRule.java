package com.cortex.base;

import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;

public final class ExcitatorySynapticPlasticityRule implements IPlasticityRule {

	private static final long MIN_ELIGIBILITY_UPDATE_STEP_NANOS = 50_000_000l;
	private static final long MIN_HOMEOSTASIS_STEP_NANOS = 5_000_000l;
	
	private float currentWeight;
	private float currentEligibility = 0.0f;
	private long lastEligibilityUpdateNanos = -1l;
	private long lastHomeostasisUpdateNanos = -1l;
	private long lastPreSpike = -1l;
	private long lastPostSpike = -1l;
	private boolean enabled = true;	// To disble plasticity
	private final float k;
	
	private final ExcitatorySynapticPlasticityConfig config;

	public ExcitatorySynapticPlasticityRule(ExcitatorySynapticPlasticityConfig config) {
		this.k = 1.0f / config.ELIGIBILITY_DECAY_NANOS;
		this.config = config;
		this.currentWeight = config.INITIAL_WEIGHT;
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
	
	// IPlasticityRule
	@Override
	public boolean hadSignificantPairing() {
	    if (lastPreSpike < 0 || lastPostSpike < 0) return false;
	    long dt = lastPostSpike - lastPreSpike;
	    return computeStdpDelta(dt) != 0f;
	}

	// compute STDP delta given dt = postTime - preTime (nanos)
	// Maths.exp uses fastExp()
	private float computeStdpDelta(long dtNanos) {
		// Branch-less version
		float dt = dtNanos;
		float sign = dt > 0 ? 1f : -1f;
		float tau  = dt > 0 ? config.TAU_PLUS : config.TAU_MINUS;
		float A    = dt > 0 ? config.A_PLUS : config.A_MINUS;
		return sign * A * Maths.exp(-Maths.abs(dt) / tau);
	}
	
	// Called by Synapse applyReward()
	public void applyReward(float reward, long now, float neuromodulator) {
		if (!enabled) return;
		if (currentEligibility == 0f) return;
		float deltaW = reward * currentEligibility * neuromodulator;
		currentWeight += deltaW;
		currentWeight = Maths.clamp(currentWeight, config.W_MIN, config.W_MAX);
		// Keep memory, don't set it to 0 immediately, consumption equivalent to reward
		float consumption = 0.2f * Maths.abs(reward);
		currentEligibility *= (1.0f - consumption);
		lastEligibilityUpdateNanos = now;
	}

	@Override
	public boolean isEligible(long now, long window) {
		if (lastEligibilityUpdateNanos <= 0L) return false;
		return currentEligibility != 0f && (now - lastEligibilityUpdateNanos) <= window;
	}

	public void onEligibilityUpdate(float delta, long now) {
		currentEligibility += delta;
		currentEligibility = Maths.clamp(currentEligibility, -1f, 1f);
		lastEligibilityUpdateNanos = now;
	}
	
	private float fastEligibilityDecay(long dt) {
	    double v = Maths.exp(-dt * k);
	    if (v < 1e-6) return 0f;
	    return (float) v;
	}
	
	// update called periodically; compute time-based decay for eligibility and homeostasis
	public void update(long now, Synapse s) {
		if (lastEligibilityUpdateNanos <= 0L) {
		    lastEligibilityUpdateNanos = now;
		    lastHomeostasisUpdateNanos = now;
		    return;
		}
		
		long dt = now - lastEligibilityUpdateNanos;
		// Sampling
	    if (dt > MIN_ELIGIBILITY_UPDATE_STEP_NANOS) {
	        currentEligibility *= fastEligibilityDecay(dt);

	        if (now - lastHomeostasisUpdateNanos > MIN_HOMEOSTASIS_STEP_NANOS) {
	            currentWeight += config.HOMEOSTATIC_RATE * (config.W_BASELINE - currentWeight);
	            currentWeight = Maths.clamp(currentWeight, config.W_MIN, config.W_MAX);
	            lastHomeostasisUpdateNanos = now;
	        }	        
	        lastEligibilityUpdateNanos = now;
	    }
	}

	public float getWeight() {
		return currentWeight;
	}

	public void enable( boolean enabled ) {
		this.enabled = enabled;
	}

	public boolean isEnabled( ) {
		return this.enabled;
	}
}

