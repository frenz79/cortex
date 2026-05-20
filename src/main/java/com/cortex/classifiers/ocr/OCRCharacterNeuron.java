package com.cortex.classifiers.ocr;

import java.util.function.Function;

import com.cortex.base.Neuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.google.common.util.concurrent.AtomicDouble;

public class OCRCharacterNeuron extends Neuron {

	private final char character;
	
	public OCRCharacterNeuron(int index, char character) {
		super(
			index,	
			8,	    // layerId
			true, 	// hasIncoming
			false, 	// hasOutgoing
			false, 	// inhibitor
			null	// position
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

	public float scoreSpikes(long wnd, long currTimeNanos) throws InterruptedException {
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
		    return score.floatValue();
	}

	@Override
	public String toString() {
		return "CharacterNeuron [" + character + "]";
	}

	@Override
	public void synapseUpdated(long now, Synapse synapse, float oldValue, float weight) {
		// opzionale: traccia o adatta plasticità locale
	}
}
