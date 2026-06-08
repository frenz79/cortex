package com.cortex.classifiers.ocr;

import java.util.function.Consumer;

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

	public float scoreSpikes(long wnd, long now) {
		AtomicDouble score = new AtomicDouble(0.0);
		for (Synapse synapse : getInSynapses()) {
			Consumer<Spike> spikesConsumer = spike -> {
				try {
					score.addAndGet(spike.signedAmplitude());
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
			synapse.forEachSpike(now, spikesConsumer);
		}
		float ret = score.floatValue();

        lastScoreTime = now;
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
