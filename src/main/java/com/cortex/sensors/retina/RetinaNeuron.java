package com.cortex.sensors.retina;

import java.util.ArrayList;
import java.util.List;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;

import net.jafama.FastMath;

public class RetinaNeuron extends AbstractNeuron {

	private float lastLuminance = 0.0f;

	private final List<Spike> spikes = new ArrayList<>();

	private static final float ON_GAIN  = 1.0f;
	private static final float OFF_GAIN = 1.0f;
	private static final float THRESHOLD = 0.01f;

	public RetinaNeuron() {
		super(false, true, false, null);
	}

	public List<Spike> getSpikes() {
		return spikes;
	}

	@Override
	public boolean process(long currTimeNanos) {
		return true;
	}
	
	public int process(long currTimeNanos, float luminance) {
		float delta = luminance - this.lastLuminance;
		int spikeCount = 0;
        float amplitude = 0f;
        		
		if (delta > THRESHOLD) { // ON channel
			spikeCount = (int)(delta * ON_GAIN * 10);
			amplitude = +delta;
		} else if (delta < -THRESHOLD) { // OFF channel
			spikeCount = (int)(-delta * OFF_GAIN * 10);
			amplitude = -delta;
		}
		for (int i = 0; i < spikeCount; i++) {
			this.spikes.add(new Spike(FastMath.abs(amplitude), currTimeNanos, false));
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void neuronFired(long now) {
		// TODO Auto-generated method stub
		
	}
}
