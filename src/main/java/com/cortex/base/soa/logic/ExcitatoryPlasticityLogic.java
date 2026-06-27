package com.cortex.base.soa.logic;

import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.utils.Maths;

public final class ExcitatoryPlasticityLogic implements IPlasticityLogic {

	private static final long MIN_ELIGIBILITY_UPDATE_STEP_NANOS = 50_000_000l;
	private static final long MIN_HOMEOSTASIS_STEP_NANOS = 5_000_000l;

	private final PlasticitySoA state;
	private final ExcitatorySynapticPlasticityConfig config;
	private final float k;

	public ExcitatoryPlasticityLogic(PlasticitySoA state, ExcitatorySynapticPlasticityConfig config) {
		this.state = state;
		this.config = config;
		this.k = 1.0f / config.ELIGIBILITY_DECAY_NANOS;
	}

	@Override
	public boolean onPreSpike(long now, int synId ) {
		state.lastPreSpike[synId] = now;

		long lastPost = state.lastPostSpike[synId];
		if (lastPost < 0L) return false;

		long dt = lastPost - now;
		float delta = computeStdpDelta(dt);

		if (delta != 0f) {
			onEligibilityUpdate(synId, delta, now);
			return true;
		}
		return false;
	}

	@Override
	public boolean onPostSpike(long now, int synId, long postTime, float postRate ) {
		state.lastPostSpike[synId] = postTime;

		long lastPre = state.lastPreSpike[synId];
		if (lastPre < 0L) return false;

		long dt = postTime - lastPre;
		float delta = computeStdpDelta(dt);

		if (delta != 0f) {
			onEligibilityUpdate(synId, delta, now);
			return true;
		}
		return false;
	}

	private float computeStdpDelta(long dtNanos) {
		float dt = dtNanos;
		float sign = Math.signum(dt);
		float tau  = dt > 0 ? config.TAU_PLUS  : config.TAU_MINUS;
		float A    = dt > 0 ? config.A_PLUS    : config.A_MINUS;
		return sign * A * Maths.exp(-Maths.abs(dt) / tau);
	}

	private void onEligibilityUpdate(int synId, float delta, long now) {
		float e = state.eligibility[synId] + delta;
		state.eligibility[synId] = Maths.clamp(e, -1f, 1f);
		state.lastEligibilityUpdate[synId] = now;
	}

	@Override
	public void applyReward(long now, int synId, float reward, float neuromodulator) {
		if (!state.isEnabled(synId)) return;

		float e = state.eligibility[synId];
		if (e == 0f) return;

		float deltaW = reward * e * neuromodulator;

		float w = state.weight[synId] + deltaW;
		w = Maths.clamp(w, config.W_MIN, config.W_MAX);
		state.weight[synId] = w;

		float consumption = 0.2f * Maths.abs(reward);
		state.eligibility[synId] *= (1.0f - consumption);
		state.lastEligibilityUpdate[synId] = now;
	}

	@Override
	public float update(long now, int synId, float postRate) {
		long last = state.lastEligibilityUpdate[synId];
		if (last <= 0L) {
			state.lastEligibilityUpdate[synId] = now;
			state.lastHomeostasisUpdate[synId] = now;
			return state.weight[synId];
		}

		long dt = now - last;

		if (dt > MIN_ELIGIBILITY_UPDATE_STEP_NANOS) {
			state.eligibility[synId] *= fastEligibilityDecay(dt);
			if (now - state.lastHomeostasisUpdate[synId] > MIN_HOMEOSTASIS_STEP_NANOS) {
				float w = state.weight[synId];
				w += config.HOMEOSTATIC_RATE * (config.W_BASELINE - w);
				state.weight[synId] = Maths.clamp(w, config.W_MIN, config.W_MAX);
				state.lastHomeostasisUpdate[synId] = now;
			}
			state.lastEligibilityUpdate[synId] = now;
		}
		return state.weight[synId];
	}

	private float fastEligibilityDecay(long dt) {
		double v = Maths.exp(-dt * k);
		if (v < 1e-6) return 0f;
		return (float) v;
	}

	@Override
	public boolean hadSignificantPairing(int synId) {
		if (state.lastPreSpike[synId] < 0 || state.lastPostSpike[synId] < 0) return false;
		long dt = state.lastPostSpike[synId] - state.lastPreSpike[synId];
		return computeStdpDelta(dt) != 0f;
	}

	@Override
	public float getWeight(int synId) {
		return state.weight[synId];
	}

	@Override
	public boolean isEligible(long now, int synId, long window) {
		if (state.lastEligibilityUpdate[synId] <= 0L) return false;
		return state.eligibility[synId] != 0f && (now - state.lastEligibilityUpdate[synId]) <= window;
	}


}
