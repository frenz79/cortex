package com.cortex.sensors.retina;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.Neuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;

public class RetinaNeuron extends Neuron {

	private float lastLuminance = 0.0f;
	
    // coda concorrente per sicurezza cross-thread
    private final Queue<Spike> spikes = new ConcurrentLinkedQueue<>();

	private static final float ON_GAIN  = 1.0f;
	private static final float OFF_GAIN = 1.0f;
	private static final float THRESHOLD = 0.01f;
	private static final int MAX_SPIKES_PER_SAMPLE = 8; // limite pratico per evitare esplosioni

	public RetinaNeuron(int index) {
		super(
				index,	
				-1,	    // layerId
				false, 	// hasIncoming
				true, 	// hasOutgoing
				false, 	// inhibitor
				null	// position
				);
	}

	@Override
	public boolean process(long currTimeNanos) {
		return true;
	}

	public int process(long currTimeNanos, float luminance) {
	    float delta = luminance - this.lastLuminance;
	    float amplitude = 0f;
	    boolean isInhibitory = false;

	    if (delta > THRESHOLD) { // ON
	        amplitude = delta * ON_GAIN;
	        isInhibitory = false;
	    } else if (delta < -THRESHOLD) { // OFF
	        amplitude = -delta * OFF_GAIN;
	        isInhibitory = true;
	    }

	    int spikeCount = 0;

	    if (amplitude > 0f) {
	        // mapping robusto
	        float raw = amplitude * 10f; // più sensibile
	        spikeCount = (int)raw;

	        float fractional = raw - spikeCount;
	        if (ThreadLocalRandom.current().nextFloat() < fractional)
	            spikeCount++;

	        spikeCount = Math.min(spikeCount, MAX_SPIKES_PER_SAMPLE);

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
	public boolean isInhibitor() {
		return false;
	}

	@Override
	public void synapseUpdated(long now, Synapse synapse, float oldValue, float weight) {
		// opzionale: traccia o adatta plasticità locale
	}
}
