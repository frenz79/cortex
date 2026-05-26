package com.cortex.classifiers.ocr;

import java.util.function.Function;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.google.common.util.concurrent.AtomicDouble;

public class OCRCharacterNeuron extends AbstractNeuron {

	private final char character;
	private float recentScore = 0f;
	private long lastScoreTime = 0;

	public OCRCharacterNeuron(int index, char character) {
		super(-1, 
				index, 
				true, 
				false, 
				false, 
				null
				);
		this.character = character;
	}

	@Override
	public boolean process(long currTimeNanos) {
		return true;
	}

	public char getCharacter() {
		return character;
	}

	public float scoreSpikes(long wnd, long currTimeNanos) {
		AtomicDouble score = new AtomicDouble(0.0);
		for (Synapse synapse : getInSynapses()) {
			Function<Spike, Spike> spikesConsumer = spike -> {
				try {
					long deltaTimeNanos = currTimeNanos - spike.getCreationTimeNanos();
					// usa il metodo del record Spike che calcola il tempo di viaggio in nanos
					long travelTimeNanos = spike.travelTimeNanos(synapse.getLength());
					if (deltaTimeNanos >= travelTimeNanos) {
						score.addAndGet(spike.getAmplitude() * spike.getSign()); // opzionale: considerare segno
						return null; // rimuovi lo spike dopo averlo consumato
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return spike; // tieni lo spike se non ancora arrivato
			};
			synapse.forEachSpike(spikesConsumer);
		}
		float ret = score.floatValue();

        lastScoreTime = currTimeNanos;
		recentScore += ret;
		
		return ret;
	}

	@Override
	public String toString() {
		return "CharacterNeuron [" + character + "]";
	}

	@Override
	public float getRecentFiringRate(long now) {
		long dt = now - lastScoreTime;
		if (dt > 100_000_000L) { // 100 ms
			recentScore = 0;
			lastScoreTime = now;
		}
		return recentScore;
	}
}
