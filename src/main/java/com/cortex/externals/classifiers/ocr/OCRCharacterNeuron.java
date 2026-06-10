package com.cortex.externals.classifiers.ocr;

import java.util.function.Consumer;

import com.cortex.base.AbstractNeuron.SynapseBranch;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.cortex.externals.AbstractExternalNeuron;
import com.google.common.util.concurrent.AtomicDouble;

public class OCRCharacterNeuron extends AbstractExternalNeuron {

	private final char character;
	private float recentScore = 0f;
	private long lastScoreTime = 0;

	public OCRCharacterNeuron(int index, char character) {
		super( index, true, false );
		this.character = character;
	}

	public char getCharacter() {
		return character;
	}

	public float scoreSpikes(long wnd, long now) {
		AtomicDouble score = new AtomicDouble(0.0);
		for (SynapseBranch synapseBranch : getInSynapseBranches()) {
			for ( Synapse synapse : synapseBranch.synapses ) {
				Consumer<Spike> spikesConsumer = spike -> {
					try {
						score.addAndGet(spike.signedAmplitude());
					} catch (Exception e) {
						e.printStackTrace();
					}
				};
				synapse.forEachSpike(now, spikesConsumer);
			}
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
