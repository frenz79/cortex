package com.cortex.classifiers.ocr;

import com.cortex.base.Neuron;
import com.cortex.commons.modules.IClassifier;

public class OCRClassifier implements IClassifier<OCRCharacterNeuron> {
	
	private long lastApply = 0l; // 75 ms
    private final long windowNanos = 75_000_000l; // 75 ms
    
	private final OCRCharacterNeuron[][] neurons;
	
	private OCRCharacterNeuron result;
	
	public OCRClassifier() {
		this.neurons = new OCRCharacterNeuron[1][26];
		int i=0;
		int counter = 0;
		for (char c = 'A'; c <= 'Z'; c++) {
			this.neurons[0][i++] = new OCRCharacterNeuron(counter++,c);
		}
	}
	
	public OCRCharacterNeuron getCharacterNeuronForLetter(char c) {
		return this.neurons[0][c-'A'];
	}
	
	@Override
	public OCRCharacterNeuron classify(long now) throws InterruptedException {
		if ( now-lastApply > windowNanos ) {
			int best = -1;
	        float bestScore = 0.0f;
	
	        for (int i = 0; i < neurons[0].length; i++) {
	            float spikes = neurons[0][i].scoreSpikes(now - windowNanos, now);
	            if (spikes > bestScore) {
	                bestScore = spikes;
	                best = i;
	            }
	        }
	        if(best>=0) {
		        this.lastApply = now;
		        this.result = neurons[0][best];
		        return this.result;
	        }
		}
		return null;
    }

	@Override
	public OCRCharacterNeuron getClassificationResult() {
		return result;
	}

	@Override
	public Neuron[][] getNeurons() {
		return neurons;
	}
}
