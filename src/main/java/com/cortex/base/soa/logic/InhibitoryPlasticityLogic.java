package com.cortex.base.soa.logic;

import com.cortex.base.plasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.utils.Maths;

public final class InhibitoryPlasticityLogic implements IPlasticityLogic {

    private final PlasticitySoA state;
    private final InhibitorySynapticPlasticityConfig config;

    public InhibitoryPlasticityLogic(PlasticitySoA state, InhibitorySynapticPlasticityConfig config) {
        this.state = state;
        this.config = config;
    }
    
    @Override
    public boolean onPostSpike(long now, int synId, long postTime, float postRate) {
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        float w = state.weight[synId] + dw;
        state.weight[synId] = Maths.clamp(w, config.W_MIN, config.W_MAX);
        return false;
    }

    @Override
    public float update(long now, int synId, float postRate) {
        float error = postRate - config.TARGET_FIRING_RATE;
        float dw = config.LEARNING_RATE * error;
        float w = state.weight[synId] + dw;
        state.weight[synId] = Maths.clamp(w, config.W_MIN, config.W_MAX);
        return w;
    }
    
    @Override
    public float getWeight(int synId) {
        return state.weight[synId];
    }

    @Override
    public boolean onPreSpike(long now, int synId ) {
        return false;
    }

    @Override
    public boolean hadSignificantPairing(int synId) {
        return false;
    }

    public boolean isEligible(long now, int synId, long window) {
        return false;
    }

    @Override
    public void applyReward(long now, int synId, float r, float neuromodulator) {
        // no-op
    }
}
