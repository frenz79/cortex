package com.cortex.base;

import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;
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

	protected final Logger logger = LogManager.getLogger(this.getClass());

	public static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	public static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	public static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = AbstractNeuron::isInhibitor;

	private final AbstractNeuron pre;
	private final AbstractNeuron post;
	private final IPlasticityRule plasticityRule;

	private static final float ETA_MYELIN = 0.0001f;	// Myelinization learning rate
	private static final float MAX_MYELIN = 1.0f;
	private final float length;
	private final long baseSpeed;						// Nanos per distance unit 
	private float myelinFactor = 0.0f;

	// Monitor synapse activity
	private static final long DECAY_INTERVAL_NANOS = 50_000_000l;
	private static final int ACTIVITY_THRESHOLD = 5;
	private int activityCounter;
	private long lastDecayTime = System.nanoTime();

	private final PriorityQueue<Spike> spikes =
			new PriorityQueue<>(Comparator.comparingLong(Spike::arrivalTime));

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

	public float getLength() {
		return length;
	}

	public long getTraversalTimeNanos(long now) {
		if (now - lastDecayTime > DECAY_INTERVAL_NANOS) {
			activityCounter = (int)(activityCounter * 0.5f);   // slow decay
			lastDecayTime = now;
		}
		return (long)(length * baseSpeed / (1.0f + myelinFactor));
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
	public void forEachSpike(long now, Function<Spike, Spike> consumer) {
		while (true) {
			Spike head = spikes.peek();
			if (head == null || head.arrivalTime() > now) break;
			consumer.apply(head);
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

	// IPlasticSynapse
	@Override
	public void onPreSpike(long now) {
		this.activityCounter++;
		if (this.plasticityRule.onPreSpike(now)) {
			EventBus.fire(EventType.SYNAPSE_SPIKED, now, this, SynapseSpikedData.preSpikeData());
		}
	}

	@Override
	public void onPostSpike(long postSpikeTime, long now) {
		this.activityCounter++;
		if ( this.plasticityRule.onPostSpike(this, postSpikeTime, now) ) {
			EventBus.fire(EventType.SYNAPSE_SPIKED, now, this, SynapseSpikedData.postSpikeData());
		}
	}

	@Override
	public void update(long t) {
		float oldValue = this.plasticityRule.getWeight();
		this.plasticityRule.update(t, this);
		EventBus.fire(EventType.SYNAPSE_UPDATED, t, this, new SynapseUpdatedData(oldValue, this.plasticityRule.getWeight()));
	}

	@Override
	public void applyReward(float deltaW, long now, float reward) {
		this.plasticityRule.applyReward(deltaW, now, reward);

		if (reward > 0 && wasFrequentlyActiveInLastWindow() && plasticityRule.hadSignificantPairing()) {
			this.myelinFactor += ETA_MYELIN * reward;
			this.myelinFactor = Maths.clamp(this.myelinFactor, 0, MAX_MYELIN);
		}
	}

	boolean wasFrequentlyActiveInLastWindow() {
		return activityCounter > ACTIVITY_THRESHOLD;
	}

	@Override
	public float getWeight() {
		return this.plasticityRule.getWeight();
	}

	public boolean isEligible(long now, long window) {
		return this.plasticityRule.isEligible(now,window);
	}

	public AbstractNeuron getTarget() {
		return post;
	}

	public AbstractNeuron getSource() {
		return pre;
	}
}
