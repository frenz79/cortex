package com.cortex.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.vecmath.Point3f;

import com.cortex.brain.GlobalContext;
import com.cortex.commons.IProcessable;

/**
 *  Event-driven, analog-spike, delayed, plastic Neuron
 * 
 * */
public class Neuron implements IProcessable {

	private static final float POTENTIAL_MAX = 3.0f;
	private static final float POTENTIAL_MIN = -2.0f;
	private static final float FIRING_THRESHOLD = 0.6f;
	private static final float POTENTIAL_ZERO = 0.0f;
	private static final long REFRACTORY_PERIOD_NANOS = TimeUnit.MILLISECONDS.toNanos(10);
	private static final float REPOLARIZATION_PER_SECOND  = 0.1f; // potential units per second
	private float potential = POTENTIAL_ZERO;
	
	private long lastProcessTime = System.nanoTime();
	private long lastSpikeTime = Long.MIN_VALUE;

	private final int layerId;
	private final int index;
	private final Point3f position;	
	private final List<Synapse> inSynapses;
	private final List<Synapse> outSynapses;
	private final int spikeSign;
	
	private static final long RATE_WINDOW = 100_000_000L; // 100 ms
	private static final double RATE_DECAY_PER_WINDOW = 0.95; // per RATE_WINDOW
	
    private float firingRate = 0.0f;
    private long lastRateUpdate = System.nanoTime();
	    
	public Neuron(int index, int layerId, boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position) {
		this.inSynapses = (hasIncoming)
			? new ArrayList<>():Collections.emptyList();
		this.outSynapses = (hasOutgoing)
			? new ArrayList<>():Collections.emptyList();
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
		this.index = index;
		this.layerId = layerId;
	}

	// Decadimento lineare verso il potenziale di riposo
	private void computeDecay( long deltaTimeNanos ) {
		if (potential == POTENTIAL_ZERO) return;
	    double seconds = deltaTimeNanos / 1_000_000_000.0;
	    double delta = REPOLARIZATION_PER_SECOND * seconds;
	    if (potential > POTENTIAL_ZERO) {
	        potential -= delta;
	        if (potential < POTENTIAL_ZERO) potential = POTENTIAL_ZERO;
	    } else {
	        potential += delta;
	        if (potential > POTENTIAL_ZERO) potential = POTENTIAL_ZERO;
	    }
	    // clamp to bounds
	    potential = Math.max(POTENTIAL_MIN, Math.min(POTENTIAL_MAX, potential));
	}
	
	/**
	 * returns:
	 * 	null if no spike has been produced
	 *  the incoming spike if it was not processed
	 *  a new Spike if a fire will occurr
	 */
	private Spike integrateInputAndFire(long currTimeNanos, long deltaTimeNanos, Spike spike, Synapse synapse) {
	    long ageNanos = currTimeNanos - spike.getCreationTimeNanos();
	    long travelTimeNanos = (long)((synapse.getLength() / spike.getSpeed()) * 1_000_000_000L); // if speed is units/sec
	    if (ageNanos >= travelTimeNanos) {
	        synapse.onPreSpike(deltaTimeNanos);
	        potential += spike.getSign() * synapse.getWeight() * spike.getAmplitude();
	        if (currTimeNanos - lastSpikeTime > REFRACTORY_PERIOD_NANOS && potential > FIRING_THRESHOLD) {
	            lastSpikeTime = currTimeNanos;
	            potential = POTENTIAL_ZERO;
	            return new Spike(synapse.getWeight() * spike.getAmplitude(), currTimeNanos, isInhibitor());
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
		long deltaTime = currTimeNanos - lastProcessTime;
		computeDecay(deltaTime);

		AtomicBoolean stayActive = new AtomicBoolean(false);
		final List<Spike> newSpikes = new ArrayList<>();
	    boolean inRefractory = (currTimeNanos - lastSpikeTime) < REFRACTORY_PERIOD_NANOS;
	    
		for ( Synapse synapse : getInSynapses() ) {
			synapse.forEachSpike( spike -> {
				try {
					Spike s = integrateInputAndFire(currTimeNanos, deltaTime, spike, synapse);
					if (s!=null ) {
						if (s==spike) {
							// We still have a spike not yet arrived...keep the synapse active
							stayActive.set(true);
						} else {
							 if (!inRefractory) {
								 newSpikes.add(s);
		                     }
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
				synapse.onPostSpike(deltaTime, currTimeNanos);
				synapse.update(deltaTime);
			}else {
	            // still call update even if no new spikes were fired to keep plasticity timing consistent
	            synapse.update(deltaTime);
	        }
		}

		this.potential = Math.min(POTENTIAL_MAX, potential);
		this.lastProcessTime = currTimeNanos;
		return stayActive.get();
	}

	public int getLayerId() {
		return layerId;
	}

	public void synapseUpdated( long time, Synapse synapse, float oldW, float newW) {
		GlobalContext.traceSynapseWeightUpdated(time, getLayerId(), oldW, newW);
	}
	
	public boolean isInhibitor() {
		return spikeSign==-1;
	}
	
	public int getSpikeSign() {
		return spikeSign;
	}
	
	public Point3f getPosition() {
		return position;
	}

	public void addIncomingSynapse(Synapse s) {
		try {
			this.inSynapses.add(s);
		} catch (Exception ex) {
			System.out.println("Failed to add incoming synapse to:"+this.toString());
			throw ex;
		}
	}

	public void addOutgoingSynapse(Synapse s) {
		try {
			this.outSynapses.add(s);
		} catch (Exception ex) {
			System.out.println("Failed to add outgoing synapse to:"+this.toString());
			throw ex;
		}
	}

	public List<Synapse> getInSynapses() {
		return inSynapses;
	}

	public void fire(List<Spike> spikes) throws InterruptedException {
		for ( Spike spike : spikes ) {
			fire( spike );
		}
	}
	
	public void fire(Spike spike) throws InterruptedException {
	//	System.out.println("Spike:"+spike);
		for ( Synapse s : this.outSynapses  ) {
			s.addSpike(spike);
			firingRate += 1.0f;
		    lastRateUpdate = spike.getCreationTimeNanos();
		    GlobalContext.setActive( s.getTarget() );
		    GlobalContext.traceNeuronFire( lastRateUpdate, this, getLayerId() );
		}
	}
	// continuous/exponential decay based on elapsed time 
	public float getRecentFiringRate(long now) {
	    if (lastRateUpdate == 0) {
	        lastRateUpdate = now;
	        return firingRate;
	    }
	    long dt = now - lastRateUpdate;
	    if (dt <= 0) return firingRate;
	    double windows = (double) dt / RATE_WINDOW;
	    firingRate *= Math.pow(RATE_DECAY_PER_WINDOW, windows);
	    lastRateUpdate = now;
	    return firingRate;
	}

	public int getIndex() {
		return index;
	}
}
