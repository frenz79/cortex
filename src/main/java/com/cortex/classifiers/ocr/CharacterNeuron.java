package com.cortex.classifiers.ocr;

import java.util.function.Function;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.google.common.util.concurrent.AtomicDouble;

public class CharacterNeuron extends AbstractNeuron {

	private final char character;
	
	public CharacterNeuron(char character) {
		super(true, false, false, null);
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
		for ( Synapse synapse : getInSynapses() ) {
			Function<Spike, Boolean> spikesConsumer = spike -> {
				try {
					long deltaTimeNanos = currTimeNanos - spike.getCreationTimeNanos();
					long travelTimeNanos = (long)(synapse.getLength() / spike.getSpeed());

					if ( deltaTimeNanos>=travelTimeNanos ) {
						score.addAndGet(spike.getAmplitude());
						return false;
					} 
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			};
			synapse.forEachSpike( spikesConsumer );	
		}
		return score.floatValue();
	}

	@Override
	public String toString() {
		return "CharacterNeuron [" + character + "]";
	}

	@Override
	public void synapseUpdated(long now, Synapse synapse, float oldValue, float weight) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void neuronFired(long now) {
		// TODO Auto-generated method stub
		
	}

}
