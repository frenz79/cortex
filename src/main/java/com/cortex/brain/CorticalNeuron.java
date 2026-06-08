package com.cortex.brain;

import java.util.concurrent.atomic.AtomicBoolean;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.commons.Maths;
import com.cortex.commons.Point3f;

/**
 *  Event-driven, analog-spike, delayed, plastic Neuron
 * 
 * */
public class CorticalNeuron extends AbstractNeuron {

	private final CorticalNeuronsConfig config;
	
	private long lastProcessTime = System.nanoTime();
	private long lastSpikeTime = 0l;
	private float potential = 0;

	public CorticalNeuron(int index, int layerId, boolean hasIncoming, boolean hasOutgoing, CorticalNeuronsConfig neuronsConfig, boolean inhibitor, Point3f position) {
		super(layerId, 
			  index, 
			  hasIncoming, 
			  hasOutgoing, 
			  inhibitor, 
			  position
		);
		this.config = neuronsConfig;
	}

	// Decadimento lineare verso il potenziale di riposo
	private void computeDecay( double deltaTimeNanos ) {
		if (potential == config.POTENTIAL_ZERO) return;
	    double delta = config.REPOLARIZATION_PER_NANOS * deltaTimeNanos;
	    if (potential > config.POTENTIAL_ZERO) {
	        potential -= delta;
	        if (potential < config.POTENTIAL_ZERO) potential = config.POTENTIAL_ZERO;
	    } else {
	        potential += delta;
	        if (potential > config.POTENTIAL_ZERO) potential = config.POTENTIAL_ZERO;
	    }
	    // clamp to bounds
	    potential = Maths.clamp(potential, config.POTENTIAL_MIN, config.POTENTIAL_MAX);
	}
	
	/**
	 * returns:
	 * 	null if no spike has been produced
	 *  the incoming spike if it was not processed
	 *  a new Spike if a fire will occurr
	 */
	private boolean integrateInputAndFire(long now, Spike spike, Synapse synapse) {
        synapse.onPreSpike(now);
	    float spikeIntensity = synapse.getWeight() * spike.amplitude();
	    potential += spike.getSign() * spikeIntensity;
	    if ( potential > config.FIRING_THRESHOLD) {
		    lastSpikeTime = now;
		    potential = config.POTENTIAL_ZERO;
		    return true;
	    }
	    return false;
	}
	
	/**
	 * Called by Thinker Neurons thread loop:
	 * - For each incoming synapse
	 *   - For each spike on the synapse
	 *     - Integrate all and grab outgoing spikes
	 *     
	 * returns TRUE if there's at least one spike not yet arrived
	 */
	@Override
	public boolean process(long now) throws InterruptedException{		
		long deltaTimeNanos = now - lastProcessTime;
		computeDecay(deltaTimeNanos);

		AtomicBoolean sendSpike = new AtomicBoolean(false);
		boolean stayActive = false;
	    boolean inRefractory = (now - lastSpikeTime) < config.REFRACTORY_PERIOD_NANOS;
	    
		for ( Synapse synapse : getInSynapses() ) {
			synapse.forEachSpike( now, (spike -> {
				try {
					boolean fire = integrateInputAndFire(now, spike, synapse);
					if (fire && !inRefractory) {
						sendSpike.set(true);
					}					
				} catch (Exception e) {
					logger.error("Exception handled in Neuron process()", e);
				}
				return null;
			} ));	
			
			if (!synapse.isEmpty()) {
				stayActive = true;
			}
			
			if (sendSpike.get()) {
			    fire(now, isInhibitor() );
			    // All synapses should be impacted -> TODO: check only active ones
				for (Synapse s : getInSynapses()) {
				    s.onPostSpike(now, now);
				}
			}

	        // still call update even if no new spikes were fired to keep plasticity timing consistent
	        synapse.update(deltaTimeNanos);
		}

		this.potential = Maths.clamp(
		    potential,
		    config.POTENTIAL_MIN,
		    config.POTENTIAL_MAX
		);

		this.lastProcessTime = now;
		return stayActive;
	}
	
	// continuous/exponential decay based on elapsed time 
	public float getRecentFiringRate(long now) {
	    long dt = now - lastRateUpdate;
	    if (dt <= 0) return firingRate;
	    
	    // Temporal normalization
	    double windows = (double) dt / config.RATE_WINDOW_NANOS;
	    firingRate *= Maths.pow(config.RATE_DECAY_PER_WINDOW, windows);
	    	    
	    // Avoid negative or too small values
	    if (firingRate < 0 || firingRate < 1e-6f) {
	    	firingRate = 0;
	    }
	    lastRateUpdate = now;
	    return firingRate;
	}
}
