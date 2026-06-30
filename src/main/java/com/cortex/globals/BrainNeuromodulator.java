package com.cortex.globals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BrainNeuromodulator {

	final Logger logger = LogManager.getLogger(this.getClass());
	/*
	private final Queue<Synapse> recentlyActiveSynapses = new ConcurrentLinkedQueue<>();
	
	public BrainNeuromodulator() {
		EventBus.addListener(EventType.SYNAPSE_SPIKED,  new EventListener() {
	
			@Override
			public void onEvent(EventType type, long time, Object source, Object data) {
				SynapseSpikedData spikedData = (SynapseSpikedData)data;
				if (source!=null) {
					recentlyActiveSynapses.add((Synapse)source);
				}
			}
		});
	}
	
	public void broadcastReward(
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
				
				if ( scaledReward!=0.0f ) {
					s.applyReward( scaledReward*sign, now, neuromodulator);
					appliedCount.incrementAndGet();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return keepAfterApply;
		};

		List<Synapse> drained = new ArrayList<>(recentlyActiveSynapses.size());
	    Synapse s;
	    while ((s = recentlyActiveSynapses.poll()) != null) {
	        drained.add(s);
	    }
	    for (Synapse syn : drained) {
	        boolean keep = true;
	        try {
	            keep = Boolean.TRUE.equals(activeSynapseConsumer.apply(syn));
	        } catch (RuntimeException ex) {
	            ex.printStackTrace();
	            // on exception, keep the synapse for retry
	            keep = true;
	        }
	        if (keep) {
	            recentlyActiveSynapses.add(syn);
	        }
	    }
	    
	//	System.out.println("Reward applied to " + appliedCount.get() + " synapses");
	}

	private static float scaleReward( int layerId, float reward ) {
		switch(layerId) {
		case -1:
			return 0.0f;
		case 1:
			return 0.05f * reward;
		case 2:
			return 0.15f * reward;
		case 3:
		case 4:
		case 5:
			return 0.30f * reward;
		default:
			return 0.1f;
		}
	}
	*/
}
