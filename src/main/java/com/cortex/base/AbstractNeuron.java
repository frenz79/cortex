package com.cortex.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.vecmath.Point3f;

import com.cortex.commons.IProcessable;

public abstract class AbstractNeuron implements IProcessable {

	private static final Map<AbstractNeuron,Long> activeNeurons = new HashMap<>(100_000);
	
	private final Point3f position;	
	private final List<Synapse> inSynapses;
	private final List<Synapse> outSynapses;
	private final int spikeSign;
	
	private static final long RATE_WINDOW = 100_000_000L; // 100 ms
	private static final float RATE_DECAY = 0.95f;
    private float firingRate = 0.0f;
    private long lastRateUpdate = 0;
	    
	protected AbstractNeuron(boolean hasIncoming, boolean hasOutgoing, boolean inhibitor, Point3f position) {
		this.inSynapses = (hasIncoming)
			? new ArrayList<>():Collections.emptyList();
		this.outSynapses = (hasOutgoing)
			? new ArrayList<>():Collections.emptyList();
		this.spikeSign = (inhibitor)?-1:1;
		this.position = position;
	}
	
	public abstract void synapseUpdated(long now, Synapse synapse, float oldValue, float weight);
	public abstract void neuronFired(long now);
	
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
		this.inSynapses.add(s);
	}

	public void addOutgoingSynapse(Synapse s) {
		this.outSynapses.add(s);
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
			activeNeurons.put( s.getTarget(), spike.getCreationTimeNanos() );
			firingRate += 1.0f;
		    lastRateUpdate = spike.getCreationTimeNanos();
		    neuronFired(spike.getCreationTimeNanos());
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
	
	public static void register( AbstractNeuron n ){
		activeNeurons.put(n ,0L);
	}
	
	public static void forEachActive( Function<AbstractNeuron, Boolean> consumer ) {
		for ( Entry<AbstractNeuron, Long> e : activeNeurons.entrySet() ) {
			if (e.getValue()>0l && !consumer.apply(e.getKey())) {
				activeNeurons.put(e.getKey(), 0L);
			}
		}
	}

	public static int getNeuronsCount() {
		return activeNeurons.size();
	}

	public List<Synapse> getOutSynapses() {
		return outSynapses;
	}
}
