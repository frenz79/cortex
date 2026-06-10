package com.cortex.externals.classifiers.ocr;

import com.cortex.base.AbstractNeuron;
import com.cortex.commons.Point3f;
import com.cortex.commons.modules.IClassifier;

public class OCRClassifier implements IClassifier<OCRCharacterNeuron> {
	
	private long lastApply = 0l; // 75 ms
    private final long windowNanos = 75_000_000l; // 75 ms
    
	private final OCRCharacterNeuron[][] neurons;
	private float smoothed[];
    private float lastConfidence = 0f;
    private long lastClassificationTime = 0L;
	private OCRCharacterNeuron result;
	
	public OCRClassifier() {
		this.neurons = new OCRCharacterNeuron[1][26];
		int i=0;
		int counter = 0;
		for (char c = 'A'; c <= 'Z'; c++) {
			this.neurons[0][i++] = new OCRCharacterNeuron(counter++, c);
		}
		this.smoothed = new float[counter];
	}
	
	public OCRCharacterNeuron getCharacterNeuronForLetter(char c) {
		return this.neurons[0][c-'A'];
	}
	
	@Override	
	public OCRCharacterNeuron classify(long now) {
	
	    if (now - lastApply <= windowNanos) return null;
	
	    int best = -1;
	    int second = -1;
	
	    float bestScore = 0f;
	    float secondScore = 0f;
	    float sum = 0f;
	
	    for (int i = 0; i < neurons[0].length; i++) {
	
	        float score = neurons[0][i].scoreSpikes(windowNanos, now);
	
	        // smoothing
	        smoothed[i] = 0.3f * score + 0.7f * smoothed[i];
	        score = smoothed[i];
	
	        sum += score;
	
	        if (score > bestScore) {
	            secondScore = bestScore;
	            second = best;
	
	            bestScore = score;
	            best = i;
	        } else if (score > secondScore) {
	            secondScore = score;
	            second = i;
	        }
	    }
	
	    if (sum <= 0f) return null;
	
	    float confidence = bestScore / sum;
	    float margin = bestScore - secondScore;
	
	    if (margin < 0.01f) return null;
	
	    lastConfidence = confidence;
	    lastClassificationTime = now;
	    lastApply = now;
	
	    result = neurons[0][best];
	    return result;
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

	@Override
	public int getSynapsesCount() {
		int ret = 0;
    	for (int x = 0; x < neurons[0].length; x++) {
    	    ret += neurons[0][x].getInSynapsesCount();
    	}
		return ret;
	}

	@Override
	public Point3f getPluginSite() {
		// TODO Auto-generated method stub
		return new Point3f(0f,0f,0f);
	}

	@Override
	public String toString() {
		return "OCRClassifier [neurons=" + neurons.length + "]";
	}
}
