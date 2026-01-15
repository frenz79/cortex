package com.cortex.classifiers.ocr;

import com.cortex.base.AbstractNeuron;
import com.cortex.classifiers.Classifier;

public class OCRClassifier implements Classifier<CharacterNeuron> {
	
	private long lastApply = 0l; // 75 ms
    private final long windowNanos = 50_000_000l; // 75 ms
    
	private final CharacterNeuron[] neurons;
	
	private CharacterNeuron result;
	
	public OCRClassifier() {
		this.neurons = new CharacterNeuron[26];
		int i=0;
		for (char c = 'A'; c <= 'Z'; c++) {
			this.neurons[i++] = new CharacterNeuron(c);
		}
	}
	
	@Override
	public void classify(long now) throws InterruptedException {
		if ( now-lastApply > windowNanos ) {
			int best = -1;
	        float bestScore = 0.0f;
	
	        for (int i = 0; i < neurons.length; i++) {
	            float spikes = neurons[i].scoreSpikes(now - windowNanos, now);
	            if (spikes > bestScore) {
	                bestScore = spikes;
	                best = i;
	            }
	        }
	        this.lastApply = now;
	        this.result = neurons[best];
		}
    }

	@Override
	public CharacterNeuron getClassificationResult() {
		return result;
	}

	@Override
	public AbstractNeuron[] getNeurons() {
		return neurons;
	}
}
