package com.cortex.externals.classifiers.ocr;

import com.cortex.base.modules.ISupervisor;
import com.cortex.base.utils.Maths;

public class OCRSupervisor implements ISupervisor<OCRCharacterNeuron> {

	private final OCRClassifier classifier;
	private volatile OCRCharacterNeuron expected;
    
 // parametri di tuning (costanti del supervisor)
    private final float WC = 0.6f;   // peso correctness
    private final float WE = 0.3f;   // peso confidence
    private final float WT = 0.1f;   // peso timing
    private final long TIMING_TAU_NANOS = 50_000_000L; // 50 ms
    
	public OCRSupervisor(OCRClassifier classifier) {
		super();
		this.classifier = classifier;
	}

	@Override
	public void process(long now) {
		/*
	    OCRCharacterNeuron winner = classifier.getClassificationResult();
	    if (winner == null || expected == null) return;

	    boolean correct = (winner == expected);
	    
	    // calcolo neuromodulator (esempio)
	    float neuromod = computeNeuromodulator(
	    	winner, 
	    	expected, 
	    	classifier.getConfidence(), 
	    	classifier.getLastClassificationTime(), 
	    	now
	    );
	    
	    float reward = (correct)?1.0f:-1.0f;
	    for (SynapseBranch sb : expected.getInSynapseBranches()) {
		   	for ( Synapse s : sb.synapses ) {
		   		s.applyReward(reward, now, neuromod);
		   	}
	    }
	    /*
	    // reward globale (consuma eligibility)
	    GlobalNeuromodulator.broadcastReward(
	        correct ? +0.5f : -0.5f,
	        now,
	        TIMING_TAU_NANOS,
	        neuromod,
	        true
	    );
	    */
	}
	
	public OCRCharacterNeuron getExpected() {
		return expected;
	}

	@Override
	public void setExpected(OCRCharacterNeuron expected) {
		this.expected = expected;
	}
	
	private float computeNeuromodulator(OCRCharacterNeuron winner, OCRCharacterNeuron expected, float confidence, long eventTime, long now) {
	    // correctness: +1 corretto, -1 errato
	    float correctness = (winner != null && expected != null && winner == expected) ? 1.0f : -1.0f;

	    // confidence normalizzata in [0,1] (assumi che caller fornisca già normalizzato)
	    float conf = Maths.clamp(confidence, 0f, 1f);

	    // timing factor: decresce esponenzialmente con delta time
	    long delta = Maths.max(0L, now - eventTime);
	    float timing = (float) Maths.exp(- (float) delta / (float) TIMING_TAU_NANOS);

	    // combinazione pesata
	    float raw = WC * correctness + WE * (2f * conf - 1f) + WT * timing; 
	    // (2*conf-1) porta confidence in [-1,1] per coerenza con correctness

	    // clamp in [-1,1]
	    return Maths.clamp( raw, -1f, 1f);
	}

	public OCRClassifier getClassifier() {
		return classifier;
	}

	@Override
	public String toString() {
		return "OCRSupervisor [expected=" + expected + ", WC=" + WC + ", WE=" + WE + ", WT=" + WT
				+ ", TIMING_TAU_NANOS=" + TIMING_TAU_NANOS + "]";
	}
}
