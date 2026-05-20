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
	private static final long TIMESTAMP_JITTER_NANOS = 1_000_000L; // fino a 1 ms di jitter (tunable)

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
		int spikeCount = 0;
		float amplitude = 0f;
		boolean isInhibitory = false;

		if (delta > THRESHOLD) { // ON channel
			spikeCount = (int)(amplitude * 10f);
			amplitude = delta * ON_GAIN;
			isInhibitory = false; // ON = eccitatorio (se la tua Spike usa isInhibitor come segno)
		} else if (delta < -THRESHOLD) { // OFF channel
			spikeCount = (int)(amplitude * 10f);
			amplitude = -delta * OFF_GAIN; // amplitude positiva
			// Decidi se OFF deve essere trattato come inibitorio o come spike con segno negativo.
			// Qui assumiamo che OFF sia inibitorio: isInhibitory = true;
			isInhibitory = true;
		}
		
		if (spikeCount > 0) {
	        // clamp amplitude (Spike lo farà comunque) e limitazione del numero di spike
	        int clampedBase = Math.min(MAX_SPIKES_PER_SAMPLE, Math.max(0, spikeCount));

	        // gestione della parte frazionaria in modo probabilistico
	        float fractional = (amplitude * 10f) - spikeCount;
	        int extra = (ThreadLocalRandom.current().nextFloat() < Math.max(0f, fractional)) ? 1 : 0;
	        int total = Math.min(MAX_SPIKES_PER_SAMPLE, clampedBase + extra);

	        // aggiungi jitter al timestamp per evitare spike perfettamente sincronizzati
	        long jitter = (TIMESTAMP_JITTER_NANOS > 0) ? ThreadLocalRandom.current().nextLong(0, TIMESTAMP_JITTER_NANOS) : 0L;
	        long spikeTime = currTimeNanos + jitter;

			for (int i = 0; i < total; i++) {
				// Creiamo Spike con amplitude positiva; il segno è determinato da isInhibitory
				// Adatta il costruttore se la tua Spike accetta un parametro "sign" esplicito.
				Spike s = new Spike(amplitude, spikeTime, isInhibitory);
				spikes.add(s);
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
