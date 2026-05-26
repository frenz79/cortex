package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.vecmath.Point3f;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.commons.Maths;

/**
 *  Event-driven, analog-spike, delayed, plastic Neuron
 * 
 * */
public class CorticalNeuron extends AbstractNeuron {

	private final CorticalNeuronsConfig config;
	
	private long lastProcessTime = 0l;
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
	private Spike integrateInputAndFire(long currTimeNanos, Spike spike, Synapse synapse) {
	    long ageNanos = currTimeNanos - spike.getCreationTimeNanos();
	    long travelTimeNanos = spike.travelTimeNanos(synapse.getLength());
	    if (ageNanos < 0) {
	        System.out.println("Spike nel futuro: age=" + ageNanos);
	    }
	    if (ageNanos >= travelTimeNanos) {
	        synapse.onPreSpike(currTimeNanos);
	        float spikeIntensity = synapse.getWeight() * spike.getAmplitude();
	        potential += spike.getSign() * spikeIntensity;
	        if (currTimeNanos - lastSpikeTime > config.REFRACTORY_PERIOD_NANOS && potential > config.FIRING_THRESHOLD) {
	            lastSpikeTime = currTimeNanos;
	            potential = config.POTENTIAL_ZERO;
	            return new Spike(spikeIntensity, currTimeNanos, isInhibitor());
	        }
	        return null;
	    } else {
	        return spike; // still in flight
	    }
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
	public boolean process(long currTimeNanos) throws InterruptedException{		
		long deltaTimeNanos = currTimeNanos - lastProcessTime;
		computeDecay(deltaTimeNanos);

		AtomicBoolean stayActive = new AtomicBoolean(false);
		final List<Spike> newSpikes = new ArrayList<>();
	    boolean inRefractory = (currTimeNanos - lastSpikeTime) < config.REFRACTORY_PERIOD_NANOS;
	    
		for ( Synapse synapse : getInSynapses() ) {
			synapse.forEachSpike( spike -> {
				try {
					Spike s = integrateInputAndFire(currTimeNanos, spike, synapse);
					if (s!=null ) {
						if (s==spike) {
							// We still have a spike not yet arrived...keep the synapse active
							stayActive.set(true);
						} else if (!inRefractory) {
							newSpikes.add(s);
						}
						return s;
					}					
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			} );	
			if (!newSpikes.isEmpty()) {
				fire(newSpikes);
				newSpikes.clear();
				// Must be called once per synapse even if fired multiple times
				// TODO: first param should be affected by travel time
				// synapse.onPostSpike(currTimeNanos, currTimeNanos);

				// All synapses should be impacted -> TODO: check only active ones
				for (Synapse s : getInSynapses()) {
				    s.onPostSpike(currTimeNanos, currTimeNanos);
				}

			}

	        // still call update even if no new spikes were fired to keep plasticity timing consistent
	        synapse.update(deltaTimeNanos);
		}

		this.potential = Maths.min(config.POTENTIAL_MAX, potential);
		this.lastProcessTime = currTimeNanos;
		return stayActive.get();
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
