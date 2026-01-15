package com.cortex.commons;

public interface IPlasticSynapse {

	void onPreSpike(long t);

	void onPostSpike(long t, long now);

	void applyReward(float r, long t);

	void update(long t);

	float getWeight();

}
