package com.cortex.classifiers.ocr;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.commons.modules.IClassifier;

public class OCRClassifier implements IClassifier<OCRCharacterNeuron> {
	
	private long lastApply = 0l; // 75 ms
    private final long windowNanos = 75_000_000l; // 75 ms
    
	private final OCRCharacterNeuron[][] neurons;
	
    private float lastConfidence = 0f;
    private long lastClassificationTime = 0L;
	private OCRCharacterNeuron result;
	
	public OCRClassifier(CorticalNeuronsConfig neuronsConfig, LayerConfig layerConfig) {
		this.neurons = new OCRCharacterNeuron[1][26];
		int i=0;
		int counter = 0;
		for (char c = 'A'; c <= 'Z'; c++) {
			this.neurons[0][i++] = new OCRCharacterNeuron(counter++, c);
		}
	}
	
	public OCRCharacterNeuron getCharacterNeuronForLetter(char c) {
		return this.neurons[0][c-'A'];
	}
	
	@Override
	public OCRCharacterNeuron classify(long now) throws InterruptedException {
	    if (now - lastApply <= windowNanos) return null;

	    int best = -1;
	    float bestScore = 0f;
	    float sum = 0f;

	    // singola passata: calcolo score e somma
	    for (int i = 0; i < neurons[0].length; i++) {
	        float score = neurons[0][i].scoreSpikes(windowNanos, now);
	        sum += score;

	        if (score > bestScore) {
	            bestScore = score;
	            best = i;
	        }
	    }

	    // confidence normalizzata
	    this.lastConfidence = (sum > 0f) ? (bestScore / sum) : 0f;
	    this.lastClassificationTime = now;

	    final float minScoreToAccept = 1e-3f;

	    if (best >= 0 && bestScore > minScoreToAccept) {
	        this.lastApply = now;
	        this.result = neurons[0][best];
	        System.out.printf("OCR classify: chosen=%c score=%.4f conf=%.3f%n",
	                result.getCharacter(), bestScore, lastConfidence);
	        return this.result;
	    }

	    return null;
	}

	public float getConfidence() {
	    return lastConfidence;
	}

	public long getLastClassificationTime() {
	    return lastClassificationTime;
	}
	
	@Override
	public OCRCharacterNeuron getClassificationResult() {
		return result;
	}

	@Override
	public AbstractNeuron[][] getNeurons() {
		return neurons;
	}
}
