package com.cortex.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
	private static final long REFRACTORY_PERIOD = 1_000_000L;
	private static final float REPOLARIZATION_PER_NANOS = 0.001f;
	private float potential = POTENTIAL_ZERO;
	
	private long lastProcessTime = System.nanoTime();
	private long lastSpikeTime = 0l;

	private final int layerId;
	private final int index;
	private final Point3f position;	
	private final List<Synapse> inSynapses;
	private final List<Synapse> outSynapses;
	private final int spikeSign;
	
	private static final long RATE_WINDOW = 100_000_000L; // 100 ms
	private static final float RATE_DECAY = 0.95f;
    private float firingRate = 0.0f;
    private long lastRateUpdate = 0;
	    
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
	private void computeDecay( long deltaTime ) {
		if (potential != POTENTIAL_ZERO) {
			if (potential > POTENTIAL_ZERO) {
				potential -= REPOLARIZATION_PER_NANOS * deltaTime;
				if (potential < POTENTIAL_ZERO) potential = POTENTIAL_ZERO;
			} else {
				potential += REPOLARIZATION_PER_NANOS * deltaTime;
				if (potential > POTENTIAL_ZERO) potential = POTENTIAL_ZERO;
			}
		}
	}
	
	private boolean integrateInputAndFire(long currTimeNanos, long deltaTime, Spike spike, Synapse synapse, AtomicBoolean hasFired) throws InterruptedException {
		long deltaTimeNanos = currTimeNanos - spike.getCreationTimeNanos();
		long travelTimeNanos = (long)(synapse.getLength() / spike.getSpeed());

		if ( deltaTimeNanos>=travelTimeNanos ) {
			synapse.onPreSpike(deltaTime);
			this.potential += (spike.getSign() * synapse.getWeight() * spike.getAmplitude());	
			if (currTimeNanos - lastSpikeTime > REFRACTORY_PERIOD && this.potential>FIRING_THRESHOLD) {
				this.lastSpikeTime = currTimeNanos;
				this.potential = POTENTIAL_ZERO;							
				this.fire( new Spike(synapse.getWeight() * spike.getAmplitude(), currTimeNanos, isInhibitor()) );
				hasFired.set(true);
			}
			return false;
		}
		return true;
	}
	
	@Override
	public boolean process(long currTimeNanos) throws InterruptedException{
		long deltaTime = currTimeNanos - lastProcessTime;
		computeDecay(deltaTime);

		AtomicBoolean allSpikesConsumed = new AtomicBoolean(true);

		for ( Synapse synapse : getInSynapses() ) {
			AtomicBoolean hasFired = new AtomicBoolean(false);
			final Function<Spike, Boolean> spikesConsumer = spike -> {
				try {
					boolean ret = integrateInputAndFire(currTimeNanos, deltaTime, spike, synapse, hasFired);
					if (ret) {
						allSpikesConsumed.set(false);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			};
			synapse.forEachSpike( spikesConsumer );	
			if (hasFired.get()) {
				// Must be called once per synapse even if fired multiple times
				synapse.onPostSpike(deltaTime, currTimeNanos);
				synapse.update(deltaTime);
			}
		}

		this.potential = Math.min(POTENTIAL_MAX, potential);
		this.lastProcessTime = currTimeNanos;

		return allSpikesConsumed.get();
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
			System.out.println("Failed to add incoming synapse to:"+this.toString());
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

	public float getRecentFiringRate( long now ) {
		long dt = now - lastRateUpdate;
		if (dt > RATE_WINDOW) {
			firingRate *= RATE_DECAY;
			lastRateUpdate = now;
		}
		return firingRate;
	}

	public List<Synapse> getOutSynapses() {
		return outSynapses;
	}

	public int getIndex() {
		return index;
	}
}
