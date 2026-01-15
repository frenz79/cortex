package com.cortex.base;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.vecmath.Point3f;

/**
 *  Event-driven, analog-spike, delayed, plastic Neuron
 * 
 * */
public class Neuron extends AbstractNeuron {

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

	public static Neuron build( int layerId, boolean inhibitor, float x, float y, float z) {
		return build(layerId, inhibitor, new Point3f(x,y,z));
	}

	public static Neuron build( int layerId, boolean inhibitor, Point3f position) {
		Neuron ret = new Neuron(layerId, inhibitor, position);
		AbstractNeuron.register(ret);
		return ret;
	}

	Neuron(int layerId, boolean inhibitor, Point3f position) {
		super(true, true, inhibitor, position);
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

	@Override
	public void neuronFired( long time ) {
		Monitor.traceNeuronFire( time, this, getLayerId() );
	}

	@Override
	public void synapseUpdated( long time, Synapse synapse, float oldW, float newW) {
		Monitor.traceSynapseWeightUpdated(time, getLayerId(), oldW, newW);
	}
}
