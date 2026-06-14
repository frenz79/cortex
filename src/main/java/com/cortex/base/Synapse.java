package com.cortex.base;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.plasticity.IPlasticSynapse;
import com.cortex.base.plasticity.IPlasticityRule;
import com.cortex.base.utils.Maths;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseSpikedData;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public final class Synapse implements IPlasticSynapse {

	static final Logger logger = LogManager.getLogger(Synapse.class);

	private static final AtomicInteger synapsesCount = new AtomicInteger(0);
	
	// Hot fields grouped together for better locality
	final class SynapseState {
		public static final int BUFFER_SIZE = 8;
		long lastDecayTime = System.nanoTime();
		float myelinFactor = 0.0f;
		int activityCounter;
		// Spikes ring buffer
		final Spike[] buffer = new Spike[BUFFER_SIZE];
		int writeIndex = 0;
		int readIndex  = 0;
	}

	// Immutable fields
	private final long baseSpeed;
	private final float length;	
	private final AbstractNeuron pre;
	private final AbstractNeuron post;
	private final IPlasticityRule plasticityRule;
	private final SynapseState state = new SynapseState();

	// Monitor synapse activity
	private static final long DECAY_INTERVAL_NANOS = 50_000_000l;
	private static final int ACTIVITY_THRESHOLD = 5;

	// Myelinization learning rate
	private static final float ETA_MYELIN = 0.0001f;	
	private static final float MAX_MYELIN = 1.0f;

	private Synapse(AbstractNeuron pre, AbstractNeuron post, float length, long baseSpeed, IPlasticityRule plasticityRule) {
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.baseSpeed = baseSpeed;
		this.plasticityRule = plasticityRule;
	}
	
	public static Synapse create(AbstractNeuron pre, AbstractNeuron post, float length, long baseSpeed, IPlasticityRule plasticityRule) {
		if (pre==null || post==null) 
			throw new RuntimeException("Invalid synapse: pre or post are null");
		if (pre==post) 
			throw new RuntimeException("Invalid synapse: pre==post");
		if (pre.getPosition().equals(post.getPosition())) 
			throw new RuntimeException("Invalid synapse: pre.position==post.position");
		if (length==0) 
			throw new RuntimeException("Invalid synapse: length");
		
		synapsesCount.incrementAndGet();
		return new Synapse(
			pre, post, length, baseSpeed, plasticityRule	
		);
	}
	
	public static int getSynapsesCount() {
		return synapsesCount.get();
	}
	
	public void addSpike(Spike spike) {
		// fifo...
		if (((state.writeIndex + 1) & (SynapseState.BUFFER_SIZE - 1)) == (state.readIndex & (SynapseState.BUFFER_SIZE - 1))) {
			state.readIndex++;
		}

		state.buffer[state.writeIndex & (SynapseState.BUFFER_SIZE - 1)] = spike;
		state.writeIndex++;
		this.getTarget().setActive(true);
	}

	public void forEachSpike(long now, Consumer<Spike> consumer) {	   
		while (state.readIndex != state.writeIndex) {
			Spike s = state.buffer[state.readIndex & (SynapseState.BUFFER_SIZE - 1)];
			// stop when a future spike is fetched
			if (s.arrivalTime() > now) {
				break;
			}
			consumer.accept(s);
			state.readIndex++;
		}
	}

	public boolean isEmpty() {
		return state.writeIndex == state.readIndex;
	}

	public final long getTraversalTimeNanos(long now) {
		if (now - state.lastDecayTime > DECAY_INTERVAL_NANOS) {
			state.activityCounter = (int)(state.activityCounter * 0.5f);
			state.lastDecayTime = now;
		}
		long delay = (long)(length * baseSpeed / (1.0f + state.myelinFactor));
		// micro-delay proportional to physical delay (5%)
		double sigma = delay * 0.05;
		long micro = (long)(Maths.nextGaussian() * sigma);
		return delay + micro;
	}

	// IPlasticSynapse
	@Override
	public final void onPreSpike(long now) {
		state.activityCounter++;
		if (this.plasticityRule.onPreSpike(now)) {
			EventBus.fire(EventType.SYNAPSE_SPIKED, now, this, SynapseSpikedData.preSpikeData());
		}
	}

	@Override
	public final void onPostSpike(long postSpikeTime, long now) {
		state.activityCounter++;
		if ( this.plasticityRule.onPostSpike(this, postSpikeTime, now) ) {
			EventBus.fire(EventType.SYNAPSE_SPIKED, now, this, SynapseSpikedData.postSpikeData());
		}
	}

	@Override
	public final void update(long t) {
		float oldValue = this.plasticityRule.getWeight();
		this.plasticityRule.update(t, this);
		EventBus.fire(EventType.SYNAPSE_UPDATED, t, this, new SynapseUpdatedData(oldValue, this.plasticityRule.getWeight()));
	}

	@Override
	public void applyReward(float deltaW, long now, float reward) {
		this.plasticityRule.applyReward(deltaW, now, reward);

		if (reward > 0.0f && wasFrequentlyActiveInLastWindow() && plasticityRule.hadSignificantPairing()) {
			state.myelinFactor += ETA_MYELIN * reward;
			state.myelinFactor = Maths.clamp(state.myelinFactor, 0, MAX_MYELIN);
		}
	}

	public final boolean wasFrequentlyActiveInLastWindow() {
		return state.activityCounter > ACTIVITY_THRESHOLD;
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
				+ ", plasticityRule=" + plasticityRule + ", state=" + state + "]";
	}
}
