package com.cortex.base;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.layers.Layer.Neighbor;
import com.cortex.commons.IPlasticSynapse;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Maths;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseSpikedData;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public final class Synapse implements IPlasticSynapse {

	static final Logger logger = LogManager.getLogger(Synapse.class);

	public static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	public static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	public static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = AbstractNeuron::isInhibitor;

	private static final float ETA_MYELIN = 0.0001f;	// Myelinization learning rate
	private static final float MAX_MYELIN = 1.0f;

	// Hot fields grouped together for better locality
	final class SynapseState {
		public static final int BUFFER_SIZE = 8;
		long lastDecayTime = System.nanoTime();
		float myelinFactor = 0.0f;
	    int activityCounter;
	    final Spike[] buffer = new Spike[BUFFER_SIZE];
	    final AtomicInteger writeIndex = new AtomicInteger();
	    final AtomicInteger readIndex  = new AtomicInteger();
	}
	
	private final long baseSpeed;
	private final float length;	
	private final AbstractNeuron pre;
	private final AbstractNeuron post;
	private final IPlasticityRule plasticityRule;
	private final SynapseState state = new SynapseState();

	// Monitor synapse activity
	private static final long DECAY_INTERVAL_NANOS = 50_000_000l;
	private static final int ACTIVITY_THRESHOLD = 5;

	public void addSpike(Spike spike) {
		int w = state.writeIndex.get();
		int r = state.readIndex.get();

		// buffer pieno → drop dello spike più vecchio
		if (((w + 1) & (SynapseState.BUFFER_SIZE - 1)) == (r & (SynapseState.BUFFER_SIZE - 1))) {
			state.readIndex.incrementAndGet();
		}

		state.buffer[w & (SynapseState.BUFFER_SIZE - 1)] = spike;
		state.writeIndex.incrementAndGet();
		this.getTarget().setActive(true);
	}
	
	public void forEachSpike(long now, Consumer<Spike> consumer) {
	    int r = state.readIndex.get();     // atomic read UNA VOLTA
	    int w = state.writeIndex.get();    // atomic read UNA VOLTA

	    // loop su spike già presenti
	    while (r != w) {
	        Spike s = state.buffer[r & (SynapseState.BUFFER_SIZE - 1)];
	        // se lo spike è nel futuro, stop
	        if (s.arrivalTime() > now) {
	            break;
	        }
	        consumer.accept(s);
	        r++; // incremento locale, NON atomico
	    }
	    // aggiorno readIndex UNA SOLA VOLTA
	    state.readIndex.set(r);
	}

	public boolean isEmpty() {
		return state.writeIndex.get() == state.readIndex.get();
	}

	public Synapse(AbstractNeuron pre, AbstractNeuron post, float length, long baseSpeed, IPlasticityRule plasticityRule) {
		super();
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.baseSpeed = baseSpeed;
		this.plasticityRule = plasticityRule;
	}
	public static int create( AbstractNeuron srcNeuron, Neighbor toNeuron, long baseSpeed, SynapsePlasticityConfig plasticityCfg ) {
		link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), baseSpeed, plasticityCfg );
		return 1;
	}

	public static int create( AbstractNeuron srcNeuron, Collection<Neighbor> toNeurons, long baseSpeed, SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor toNeuron : toNeurons) {
			link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), baseSpeed, plasticityCfg );
		}
		return toNeurons.size();
	}

	public static int create( Neighbor srcNeuron, AbstractNeuron toNeuron, long baseSpeed, SynapsePlasticityConfig plasticityCfg ) {
		link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), baseSpeed, plasticityCfg );
		return 1;
	}

	public static int create( Collection<Neighbor> srcNeurons, AbstractNeuron toNeuron, long baseSpeed, SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor srcNeuron : srcNeurons) {
			link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), baseSpeed, plasticityCfg );
		}
		return srcNeurons.size();
	}

	public static int create( AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, long baseSpeed, SynapsePlasticityConfig plasticityCfg ) {
		link( srcNeuron, toNeuron, distance, baseSpeed, plasticityCfg );
		return 1;
	}

	private static void link(AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, long baseSpeed, SynapsePlasticityConfig plasticityCfg) {
		Synapse s = new Synapse( 
				srcNeuron, 
				toNeuron, 
				distance, 
				baseSpeed,
				srcNeuron.isInhibitor() 
				?new InhibitorySynapticPlasticityRule( plasticityCfg.inhibitory())
						:new ExcitatorySynapticPlasticityRule( plasticityCfg.excitatory())
				);
		toNeuron.addIncomingSynapse( s );
		srcNeuron.addOutgoingSynapse( s ); 
	}

	public final float getLength() {
		return length;
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

	/**
	 * Iterate spikes in FIFO order.
	 *
	 * Consumer contract:
	 *  - return the same Spike instance to keep it (not yet arrived)
	 *  - return any other value (including null) to remove the head spike
	 *
	 * The consumer must be fast and non-blocking; heavy work should be deferred.
	 */
/*
	private final PriorityBlockingQueue<Spike> spikes =
				new PriorityBlockingQueue<>(8,Comparator.comparingLong(Spike::arrivalTime));

	public void forEachSpike(long now, Consumer<Spike> consumer) {
		while (true) {
			Spike head = spikes.peek();
			if (head == null) break;
			if (head.arrivalTime() > now) break;
			consumer.accept(head)
			spikes.poll();
		}
	}

	public void addSpike(Spike spike) throws InterruptedException {
		this.spikes.add(spike);
		this.getTarget().setActive(true);
	}

	public boolean isEmpty() {
		return spikes.isEmpty();
	}
	*/
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

	@Override
	public int hashCode() {
		return Objects.hash(state.activityCounter, length, post, pre);
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
		return state.activityCounter == other.state.activityCounter
				&& Float.floatToIntBits(length) == Float.floatToIntBits(other.length)
				&& Objects.equals(post, other.post) && Objects.equals(pre, other.pre);
	}
}
