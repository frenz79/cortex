package com.cortex.base;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.plasticity.ExcitatorySynapticPlasticityRule;
import com.cortex.base.plasticity.IPlasticSynapse;
import com.cortex.base.plasticity.IPlasticityRule;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityRule;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.utils.Maths;
import com.cortex.brain.CorticalNeuron;
import com.cortex.brain.HemisphereContext;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseSpikedData;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public final class Synapse implements IPlasticSynapse {

	static final Logger logger = LogManager.getLogger(Synapse.class);

	private static final AtomicInteger synapsesCount = new AtomicInteger(0);
	
	public static final int BUFFER_SIZE = 8;
	final Spike[] buffer = new Spike[BUFFER_SIZE];
		
	// Immutable fields
	private final int index;	
	private final long baseSpeed;
	private final float length;	
	private final AbstractNeuron pre;
	private final AbstractNeuron post;
	private final IPlasticityRule plasticityRule;
	private final int hemisphereId;

	// Monitor synapse activity
	private static final long DECAY_INTERVAL_NANOS = 50_000_000l;
	private static final int ACTIVITY_THRESHOLD = 5;

	// Myelinization learning rate
	private static final float ETA_MYELIN = 0.0001f;	
	private static final float MAX_MYELIN = 1.0f;

	private Synapse(int index, int hemisphereId, AbstractNeuron pre, AbstractNeuron post, float length, long baseSpeed, IPlasticityRule plasticityRule) {
		this.index = index;
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.baseSpeed = baseSpeed;
		this.plasticityRule = plasticityRule;
		this.hemisphereId = hemisphereId;
	}
	
	public void init() {
		HemisphereContext.get(hemisphereId).synapseSoA.lastDecayTime[index] = System.nanoTime();
	}
	
	public static void destroy(Synapse s) {
		if (s!=null) {
			s=null;
			synapsesCount.decrementAndGet();
		}
	}
	
	public static Synapse create( int hemisphereId, AbstractNeuron pre, AbstractNeuron post, float length, long baseSpeed, SynapsePlasticityConfig synCfg) {
		if (pre==null || post==null) 
			throw new RuntimeException("Invalid synapse: pre or post are null");
		if (pre==post) 
			throw new RuntimeException("Invalid synapse: pre==post");
		if (pre.getPosition()!=null && post.getPosition()!=null && pre.getPosition().equals(post.getPosition())) 
			throw new RuntimeException("Invalid synapse: pre.position==post.position");
		if (length==0) 
			throw new RuntimeException("Invalid synapse: length");
		
		IPlasticityRule synPlast = pre.isInhibitor() 
				?new InhibitorySynapticPlasticityRule( synCfg.inhibitory())
				:new ExcitatorySynapticPlasticityRule( synCfg.excitatory());
		
		int index = synapsesCount.getAndIncrement();
		return new Synapse(
			index, hemisphereId, pre, post, length, baseSpeed, synPlast	
		);
	}
	
	public static int getSynapsesCount() {
		return synapsesCount.get();
	}
	
	public void addSpike(long now, Spike spike) {
//		if (pre.getLayer()!=null && post.getLayer()!=null && pre.getLayerId() == 0 && post.getLayerId() == 1) {
//		    logger.info("--> Spike L0→L1 scheduled: pre={} post={}",pre.getIndex(), post.getIndex());
//		}
		
		int[] wIdx = HemisphereContext.get(hemisphereId).synapseSoA.writeIndex;
		int[] rIdx = HemisphereContext.get(hemisphereId).synapseSoA.readIndex;
		
		// fifo...
		if (((wIdx[index] + 1) & (BUFFER_SIZE - 1)) == (rIdx[index] & (BUFFER_SIZE - 1))) {
			rIdx[index]++;
		}

		buffer[wIdx[index] & (BUFFER_SIZE - 1)] = spike;
		wIdx[index]++;
		
		long dt = spike.arrivalTime() - now;
		if (dt > 1_000_000) {
		    logger.warn("Spike delay too large: {} ns", dt);
		}
		
		post.setActive(true);
	}
	
	public void accumulateSpikes(long now) {
		post.setActive(false);
		int[] wIdx = HemisphereContext.get(hemisphereId).synapseSoA.writeIndex;
		int[] rIdx = HemisphereContext.get(hemisphereId).synapseSoA.readIndex;
		int branchIdx = HemisphereContext.get(hemisphereId).synapseSoA.branchIndex[index];
		 
		while (rIdx[index] != wIdx[index]) {
	    	Spike s = buffer[rIdx[index] & (BUFFER_SIZE - 1)];
			if (s == null) {
			    break;
			}
			// stop when a future spike is fetched
			if (s.arrivalTime() > now) {
				post.setActive(true);
				break;
			}
			HemisphereContext.get(hemisphereId).synapseBranchSoA.branchPotential[branchIdx] += s.signedAmplitude();
	        onPreSpike(now);
	        post.setActive(true);
	        rIdx[index]++;
	    }
	}
	
/*
	public void forEachSpike(long now, Consumer<Spike> consumer) {	   
		while (rIdx[index] != wIdx[index]) {
			Spike s = buffer[rIdx[index] & (BUFFER_SIZE - 1)];
			if (s == null) {
				post.setActive(false);
			    break;
			}
			
			// stop when a future spike is fetched
			if (s.arrivalTime() > now) {
				post.setActive(true);
				break;
			}
			consumer.accept(s);
			rIdx[index]++;
		}
	}
*/
	public boolean isEmpty() {
		int[] wIdx = HemisphereContext.get(hemisphereId).synapseSoA.writeIndex;
		int[] rIdx = HemisphereContext.get(hemisphereId).synapseSoA.readIndex;
		return wIdx[index] == rIdx[index];
	}
	
	public boolean hasFutureSpikes(long now) {
		int[] wIdx = HemisphereContext.get(hemisphereId).synapseSoA.writeIndex;
		int[] rIdx = HemisphereContext.get(hemisphereId).synapseSoA.readIndex;

	    if (wIdx[index] == rIdx[index]) return false;
	    Spike s = buffer[rIdx[index] & (BUFFER_SIZE - 1)];
	    return s.arrivalTime() > now;
	}

	public final long getTraversalTimeNanos(long now) {
		SynapseSoA stateBuff = HemisphereContext.get(hemisphereId).synapseSoA;
		
		if (now - stateBuff.lastDecayTime[index] > DECAY_INTERVAL_NANOS) {
			stateBuff.activityCounter[index] = (int)(stateBuff.activityCounter[index] * 0.5f);
			stateBuff.lastDecayTime[index] = now;
		}
		long delay = (long)(length * baseSpeed / (1.0f + stateBuff.myelinFactor[index]));
		// micro-delay proportional to physical delay (5%)
		double sigma = delay * 0.05;
		long micro = (long)(Maths.nextGaussian() * sigma);
		return delay + micro;
	}

	// IPlasticSynapse
	@Override
	public final void onPreSpike(long now) {
		HemisphereContext.get(hemisphereId).synapseSoA.activityCounter[index]++;
		if (this.plasticityRule.onPreSpike(now)) {
			EventBus.fire(EventType.SYNAPSE_SPIKED, now, this, SynapseSpikedData.preSpikeData());
		}
	}

	@Override
	public final void onPostSpike(long postSpikeTime, long now) {
		HemisphereContext.get(hemisphereId).synapseSoA.activityCounter[index]++;
		if ( this.plasticityRule.onPostSpike(this, postSpikeTime, now) ) {
			EventBus.fire(EventType.SYNAPSE_SPIKED, now, this, SynapseSpikedData.postSpikeData());
		}
	}

	@Override
	public final void update(long t) {
		float oldValue = this.plasticityRule.getWeight();
		float newValue = this.plasticityRule.update(t, this);
		if (oldValue!=newValue) {
			EventBus.fire(EventType.SYNAPSE_UPDATED, t, this, new SynapseUpdatedData(oldValue, newValue));
		}
	}

	@Override
	public void applyReward(float deltaW, long now, float reward) {
		this.plasticityRule.applyReward(deltaW, now, reward);
		float[] myelinFactors = HemisphereContext.get(hemisphereId).synapseSoA.myelinFactor;
		
		if (reward > 0.0f && wasFrequentlyActiveInLastWindow() && plasticityRule.hadSignificantPairing()) {
			myelinFactors[index] += ETA_MYELIN * reward;
			myelinFactors[index] = Maths.clamp(myelinFactors[index], 0, MAX_MYELIN);
		}
	}

	public final boolean wasFrequentlyActiveInLastWindow() {
		return HemisphereContext.get(hemisphereId).synapseSoA.activityCounter[index] > ACTIVITY_THRESHOLD;
	}

	@Override
	public final float getWeight() {
		return this.plasticityRule.getWeight();
	}

	public final boolean isEligible(long now, long window) {
		return this.plasticityRule.isEligible(now,window);
	}

	public AbstractNeuron getTarget() {
		return post;
	}

	public AbstractNeuron getSource() {
		return pre;
	}
	
	public final float getLength() {
		return length;
	}
	
	public void setBranch(int branchIndex) {
		HemisphereContext.get(hemisphereId).synapseSoA.branchIndex[index] = branchIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(post, pre);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Synapse other = (Synapse) obj;
		return Objects.equals(post, other.post) && Objects.equals(pre, other.pre);
	}

	@Override
	public String toString() {
		return "Synapse [baseSpeed=" + baseSpeed + ", length=" + length + ", pre=" + pre + ", post=" + post
				+ ", plasticityRule=" + plasticityRule + "]";
	}
}
