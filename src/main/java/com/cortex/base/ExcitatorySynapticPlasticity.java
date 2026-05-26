package com.cortex.base;

import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;

public final class ExcitatorySynapticPlasticity implements IPlasticityRule {

	private float initialDelay;
	private float currentWeight;
	private float currentEligibility = 0.0f;
	private long lastEligibilityUpdateNanos = -1l;
	private long lastPreSpike = -1l;
	private long lastPostSpike = -1l;
	private boolean enabled = true;	// To disble plasticity

	private final ExcitatorySynapticPlasticityConfig config;

	public ExcitatorySynapticPlasticity(ExcitatorySynapticPlasticityConfig config) {
		this.config = config;
		this.currentWeight = config.INITIAL_WEIGHT;
		this.initialDelay = config.INITIAL_DELAY;
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
			return (float) (config.A_PLUS * Maths.exp(-dt / (double) config.TAU_PLUS));
		} else {
			// dt <= 0 : depression
			return (float) (-config.A_MINUS * Maths.exp(dt / (double) config.TAU_MINUS));
		}
	}
	
	// Called by Synapse applyReward()
	public void applyReward(float reward, long now, float neuromodulator) {
		if (!enabled) return;
		if (currentEligibility == 0f) return;
		float deltaW = reward * currentEligibility * neuromodulator;
		currentWeight += deltaW;
		currentWeight = Maths.clamp(currentWeight, config.W_MIN, config.W_MAX);
		// Keep memory, don't set it to 0 immediately, consumption equivalent to reward
		float consumption = 0.2f * Math.abs(reward);
		currentEligibility *= (1.0f - consumption);
		lastEligibilityUpdateNanos = now;
	}
	
	// Called by Synapse in onPostSpike()
	// TODO: currentEligibility can be 0
	public void updateDelay(float reward) {
		if (!config.PLASTIC_DELAY) return;
		initialDelay += reward * currentEligibility * 0.1f;
		initialDelay = Maths.clamp(initialDelay, config.PLASTIC_DELAY_MIN, config.PLASTIC_DELAY_MAX);
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

	// update called periodically; compute time-based decay for eligibility and homeostasis
	public void update(long now, Synapse s) {
		if (lastEligibilityUpdateNanos > 0L) {
			long dt = now - lastEligibilityUpdateNanos; // nanos
			double decayFactor = Maths.pow(config.ELIGIBILITY_DECAY_NANOS, dt);
			// if very small, zero it
			currentEligibility = Maths.zeroIfSmall(currentEligibility*(float) decayFactor);
		}

		// homeostatic drift towards baseline (time-independent small step)
		currentWeight += config.HOMEOSTATIC_RATE * (config.W_BASELINE - currentWeight);
		currentWeight = Maths.clamp(currentWeight, config.W_MIN, config.W_MAX);
	}

	public float getWeight() {
		return currentWeight;
	}

	public float getDelay() {
		return initialDelay;
	}
	
	public void enable( boolean enabled ) {
		this.enabled = enabled;
	}

	public boolean isEnabled( ) {
		return this.enabled;
	}
}

