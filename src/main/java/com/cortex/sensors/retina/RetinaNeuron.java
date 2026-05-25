package com.cortex.sensors.retina;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.commons.Maths;

public class RetinaNeuron extends AbstractNeuron {

	private float lastLuminance = Float.NaN;

	private final Queue<Spike> spikes = new ConcurrentLinkedQueue<>();

	private final RetinaNeuronConfig retinaNeuronConfig;

	public RetinaNeuron(int index, RetinaNeuronConfig retinaNeuronConfig) {
		super(-1, 
				index, 
				false, 
				true, 
				false, 
				null
				);
		this.retinaNeuronConfig = retinaNeuronConfig;
	}

	@Override
	public final boolean process(long currTimeNanos) {
		return true;
	}

	public int process(long currTimeNanos, float luminance) {
		// Avoids massive ON/OFF on first frame
		if (Float.isNaN(lastLuminance)) {
		    lastLuminance = luminance;
		    return 0;
		}
		
		float delta = luminance - this.lastLuminance;
		float amplitude = 0f;
		boolean isInhibitory = false;

		if (delta > this.retinaNeuronConfig.THRESHOLD) { // ON
			amplitude = delta * this.retinaNeuronConfig.ON_GAIN;
			isInhibitory = false;
		} else if (delta < -this.retinaNeuronConfig.THRESHOLD) { // OFF
			amplitude = -delta * this.retinaNeuronConfig.OFF_GAIN;
			isInhibitory = true;
		}

		int spikeCount = 0;

		if (amplitude > 0f) {
			// mapping robusto
			float raw = amplitude * 0.3f; // più sensibile
			spikeCount = (int)raw;

			float fractional = raw - spikeCount;
			if (ThreadLocalRandom.current().nextFloat() < fractional)
				spikeCount++;

			spikeCount = Maths.min(spikeCount, this.retinaNeuronConfig.MAX_SPIKES_PER_SAMPLE);

			for (int i = 0; i < spikeCount; i++) {
				spikes.add(new Spike(amplitude, currTimeNanos, isInhibitory));
			}
		}

		this.lastLuminance = luminance;
		return spikeCount;
	}

	public List<Spike> drainSpikes() {
		List<Spike> out = new ArrayList<>(spikes);
		spikes.clear();
		return out;
	}

	@Override
	public float getRecentFiringRate(long now) {
		return spikes.size();
	}
}
