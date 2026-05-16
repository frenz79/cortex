package com.cortex.brain;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.cortex.base.Neuron;
import com.cortex.base.Synapse;
import com.cortex.classifiers.ocr.OCRCharacterNeuron;

public class GlobalNeuromodulator {


	public static void broadcastReward(
			float reward,
			long now,
			long window
			) {
		AtomicInteger appliedCount = new AtomicInteger(0);
		
		Function<Synapse, Boolean> activeSynapseConsumer = s -> {
			try {
				if (!s.isEligible(now, window)) {
					return false;
				}
				int sign = 1;
				int layerId = -1;

				if ( s.getTarget() instanceof Neuron ) {
					Neuron n = (Neuron)s.getTarget();

					if ( n.isInhibitor() ) {
						sign = -1;
					}

					layerId = n.getLayerId();
				} else  if ( s.getTarget() instanceof OCRCharacterNeuron ) {
					layerId = 8;
				}

				if ( layerId>=0 ) {
					float scaledReward = scaleReward(((Neuron)(s.getTarget())).getLayerId(),reward);
					if ( scaledReward>0.0f ) {
						s.applyReward( scaledReward*sign, now);
						appliedCount.incrementAndGet();
					}
				}
				return false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		};

		GlobalContext.forEachActiveSynapse( activeSynapseConsumer );
		System.out.println("Reward applied to " + appliedCount.get() + " synapses");
	}

	private static float scaleReward( int layerId, float reward ) {
		switch(layerId) {
		case 1:
			return 0.05f * reward;
		case 2:
			return 0.15f * reward;
		case 3:
			return 0.30f * reward;
		case 8:
			return 1.00f * reward;
		default:
			return 0.0f;
		}
	}
}
