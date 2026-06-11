package com.cortex.base.plasticity;

public interface IPlasticSynapse {

	void onPreSpike(long t);

	void onPostSpike(long t, long now);

	void applyReward(float r, long t, float neuromodulator);

	void update(long t);

	float getWeight();

}
