package com.cortex.externals.sensors.retina;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.externals.AbstractExternalNeuron;
import com.cortex.base.utils.Maths;

public class RetinaNeuron extends AbstractExternalNeuron {

	private float lastLuminance = Float.NaN;

	private final Queue<Float> spikesAmplitude = new ConcurrentLinkedQueue<>();

	private final RetinaNeuronConfig retinaNeuronConfig;

	private float firingRate;
	private long lastUpdate;
	
	public RetinaNeuron(int index, RetinaNeuronConfig retinaNeuronConfig) {
		super( index, false, true);
		this.retinaNeuronConfig = retinaNeuronConfig;
	}
	
	public int process(long currTimeNanos, float luminance) {
		// Avoids massive ON/OFF on first frame
		if (Float.isNaN(lastLuminance)) {
		    lastLuminance = luminance;
		    return 0;
		}
		
		float delta = luminance - this.lastLuminance;
		float amplitude = 0f;

		if (delta > this.retinaNeuronConfig.THRESHOLD) { // ON
			amplitude = delta * this.retinaNeuronConfig.ON_GAIN;
		} else if (delta < -this.retinaNeuronConfig.THRESHOLD) { // OFF
			amplitude = -delta * this.retinaNeuronConfig.OFF_GAIN;
		}
		
		int spikeCount = 0;

		if (amplitude != 0f) {
			// mapping robusto
			float raw = amplitude * 0.3f; // più sensibile
			spikeCount = (int)raw;

			float fractional = raw - spikeCount;
			if (ThreadLocalRandom.current().nextFloat() < fractional)
				spikeCount++;

			spikeCount = Maths.min(spikeCount, this.retinaNeuronConfig.MAX_SPIKES_PER_SAMPLE);

			for (int i = 0; i < spikeCount; i++) {
				spikesAmplitude.add(amplitude);
			}
		}

		this.lastLuminance = luminance;
		this.firingRate += spikeCount;
		return spikeCount;
	}
	
	public List<Float> drainSpikes() {
		List<Float> out = new ArrayList<>(spikesAmplitude);
		spikesAmplitude.clear();
		return out;
	}

	@Override
	public float getRecentFiringRate(long now) {
	    long dt = now - lastUpdate;
	    firingRate *= Math.exp(-dt / this.retinaNeuronConfig.TAU);
	    lastUpdate = now;
	    return firingRate;
	}
}
