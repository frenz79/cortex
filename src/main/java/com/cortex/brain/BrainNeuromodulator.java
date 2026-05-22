package com.cortex.brain;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.globals.GlobalContext;

public class BrainNeuromodulator {

	public static void broadcastReward(
			float reward,
			long now,
			long window,
			float neuromodulator,
			boolean keepAfterApply
	) {
		AtomicInteger appliedCount = new AtomicInteger(0);

		Function<Synapse, Boolean> activeSynapseConsumer = s -> {
			try {
				if (!s.isEligible(now, window)) {
					return false;
				}

				AbstractNeuron n = s.getTarget();
				int sign = n.getSpikeSign();
				float scaledReward = scaleReward(((CorticalNeuron)(s.getTarget())).getLayerId(),reward);
				
				if ( scaledReward>0.0f ) {
					s.applyReward( scaledReward*sign, now, neuromodulator);
					appliedCount.incrementAndGet();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return keepAfterApply;
		};

		GlobalContext.forEachActiveSynapse( activeSynapseConsumer );
	//	System.out.println("Reward applied to " + appliedCount.get() + " synapses");
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
