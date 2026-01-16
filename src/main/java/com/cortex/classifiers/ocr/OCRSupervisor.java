package com.cortex.classifiers.ocr;

import com.cortex.base.Synapse;
import com.cortex.commons.modules.ISupervisor;

public class OCRSupervisor implements ISupervisor<CharacterNeuron> {

	private final OCRClassifier classifier;
	private CharacterNeuron expected;

	public OCRSupervisor(OCRClassifier classifier) {
		super();
		this.classifier = classifier;
	}

	@Override
	public void process(long now) {
		CharacterNeuron winner = classifier.getClassificationResult();
		if (winner == null) {
			return;
		}

		reward( now, 1.5f, getExpected() );
		
		if (winner != getExpected()){
			reward( now, -1.5f, winner );
		}
	}

	private static final void reward( long now, float reward, CharacterNeuron n ) {
		for (Synapse s : n.getInSynapses()) {
			s.applyReward(reward, now);
		}
	}
	
	public CharacterNeuron getExpected() {
		return expected;
	}

	@Override
	public void setExpected(CharacterNeuron expected) {
		this.expected = expected;
	}
}
