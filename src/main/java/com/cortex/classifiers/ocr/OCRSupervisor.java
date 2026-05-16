package com.cortex.classifiers.ocr;

import com.cortex.base.Synapse;
import com.cortex.brain.GlobalNeuromodulator;
import com.cortex.commons.modules.ISupervisor;

public class OCRSupervisor implements ISupervisor<OCRCharacterNeuron> {

	private final OCRClassifier classifier;
	private OCRCharacterNeuron expected;

	public OCRSupervisor(OCRClassifier classifier) {
		super();
		this.classifier = classifier;
	}

	@Override
	public void process(long now) {
		OCRCharacterNeuron winner = classifier.getClassificationResult();
		if (winner == null) {
			return;
		}

		reward( now, 1.5f, getExpected() );
		
		if (winner != getExpected()){
			reward( now, -1.5f, winner );
		}
		
		GlobalNeuromodulator.broadcastReward(
			(winner == getExpected()) ? +0.5f : -0.5f,
		    now,
		    50_000_000L // 50 ms
		);
	}

	private static final void reward( long now, float reward, OCRCharacterNeuron n ) {
		for (Synapse s : n.getInSynapses()) {
			s.applyReward(reward, now);
		}
	}
	
	public OCRCharacterNeuron getExpected() {
		return expected;
	}

	@Override
	public void setExpected(OCRCharacterNeuron expected) {
		this.expected = expected;
	}
}
