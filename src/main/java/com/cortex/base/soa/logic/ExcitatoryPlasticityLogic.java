package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.utils.Maths;

public final class ExcitatoryPlasticityLogic implements IPlasticityLogic {

	private static final long MIN_ELIGIBILITY_UPDATE_STEP_NANOS = 50_000_000l;
	private static final long MIN_HOMEOSTASIS_STEP_NANOS = 5_000_000l;

	private final PlasticitySoA plasticitySoA;
	private final ExcitatorySynapticPlasticityConfig config;
	private final float k;

	public ExcitatoryPlasticityLogic( 
		ExcitatorySynapticPlasticityConfig config, 
		PlasticitySoA plasticitySoA
	) {
		Objects.nonNull(plasticitySoA);
		
		this.plasticitySoA = plasticitySoA;
		this.config = config;
		this.k = 1.0f / config.ELIGIBILITY_DECAY_NANOS;
	}

	@Override
	public boolean onPreSpike(long now, int synId ) {
		plasticitySoA.lastPreSpike[synId] = now;

		long lastPost = plasticitySoA.lastPostSpike[synId];
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
		plasticitySoA.lastPostSpike[synId] = postTime;

		long lastPre = plasticitySoA.lastPreSpike[synId];
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
		float e = plasticitySoA.eligibility[synId] + delta;
		plasticitySoA.eligibility[synId] = Maths.clamp(e, -1f, 1f);
		plasticitySoA.lastEligibilityUpdate[synId] = now;
	}

	@Override
	public void applyReward(long now, int synId, float reward, float neuromodulator) {
		if (!plasticitySoA.isEnabled(synId)) return;

		float e = plasticitySoA.eligibility[synId];
		if (e == 0f) return;

		float deltaW = reward * e * neuromodulator;

		float w = plasticitySoA.weight[synId] + deltaW;
		w = Maths.clamp(w, config.W_MIN, config.W_MAX);
		plasticitySoA.weight[synId] = w;

		float consumption = 0.2f * Maths.abs(reward);
		plasticitySoA.eligibility[synId] *= (1.0f - consumption);
		plasticitySoA.lastEligibilityUpdate[synId] = now;
	}

	@Override
	public float update(long now, int synId, float postRate) {
		long last = plasticitySoA.lastEligibilityUpdate[synId];
		if (last <= 0L) {
			plasticitySoA.lastEligibilityUpdate[synId] = now;
			plasticitySoA.lastHomeostasisUpdate[synId] = now;
			return plasticitySoA.weight[synId];
		}

		long dt = now - last;

		if (dt > MIN_ELIGIBILITY_UPDATE_STEP_NANOS) {
			plasticitySoA.eligibility[synId] *= fastEligibilityDecay(dt);
			if (now - plasticitySoA.lastHomeostasisUpdate[synId] > MIN_HOMEOSTASIS_STEP_NANOS) {
				float w = plasticitySoA.weight[synId];
				w += config.HOMEOSTATIC_RATE * (config.W_BASELINE - w);
				plasticitySoA.weight[synId] = Maths.clamp(w, config.W_MIN, config.W_MAX);
				plasticitySoA.lastHomeostasisUpdate[synId] = now;
			}
			plasticitySoA.lastEligibilityUpdate[synId] = now;
		}
		return plasticitySoA.weight[synId];
	}

	private float fastEligibilityDecay(long dt) {
		double v = Maths.exp(-dt * k);
		if (v < 1e-6) return 0f;
		return (float) v;
	}

	@Override
	public boolean hadSignificantPairing(int synId) {
		if (plasticitySoA.lastPreSpike[synId] < 0 || plasticitySoA.lastPostSpike[synId] < 0) return false;
		long dt = plasticitySoA.lastPostSpike[synId] - plasticitySoA.lastPreSpike[synId];
		return computeStdpDelta(dt) != 0f;
	}

	@Override
	public float getWeight(int synId) {
		return plasticitySoA.weight[synId];
	}

	@Override
	public boolean isEligible(long now, int synId, long window) {
		if (plasticitySoA.lastEligibilityUpdate[synId] <= 0L) return false;
		return plasticitySoA.eligibility[synId] != 0f && (now - plasticitySoA.lastEligibilityUpdate[synId]) <= window;
	}


}
