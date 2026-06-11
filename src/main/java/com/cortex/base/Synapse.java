package com.cortex.base;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.layers.Neighbor;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityRule;
import com.cortex.base.plasticity.IPlasticSynapse;
import com.cortex.base.plasticity.IPlasticityRule;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityRule;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.utils.Maths;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseSpikedData;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public final class Synapse implements IPlasticSynapse {

	static final Logger logger = LogManager.getLogger(Synapse.class);

	private static final float ETA_MYELIN = 0.0001f;	// Myelinization learning rate
	private static final float MAX_MYELIN = 1.0f;
	
	// Hot fields grouped together for better locality
	final class SynapseState {
		public static final int BUFFER_SIZE = 8;
		long lastDecayTime = System.nanoTime();
		float myelinFactor = 0.0f;
	    int activityCounter;
	    final Spike[] buffer = new Spike[BUFFER_SIZE];
	    int writeIndex = 0;
	    int readIndex  = 0;
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
		// buffer pieno → drop dello spike più vecchio
		if (((state.writeIndex + 1) & (SynapseState.BUFFER_SIZE - 1)) == (state.readIndex & (SynapseState.BUFFER_SIZE - 1))) {
			state.readIndex++;
		}

		state.buffer[state.writeIndex & (SynapseState.BUFFER_SIZE - 1)] = spike;
		state.writeIndex++;
		this.getTarget().setActive(true);
	}
	
	public void forEachSpike(long now, Consumer<Spike> consumer) {	   
	    // loop su spike già presenti
	    while (state.readIndex != state.writeIndex) {
	        Spike s = state.buffer[state.readIndex & (SynapseState.BUFFER_SIZE - 1)];
	        // se lo spike è nel futuro, stop
	        if (s.arrivalTime() > now) {
	            break;
	        }
	        consumer.accept(s);
	        state.readIndex++; // incremento locale, NON atomico
	    }
	}

	public boolean isEmpty() {
		return state.writeIndex == state.readIndex;
	}

	private Synapse(AbstractNeuron pre, AbstractNeuron post, float length, long baseSpeed, IPlasticityRule plasticityRule) {
		super();
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.baseSpeed = baseSpeed;
		this.plasticityRule = plasticityRule;
	}

	public static int create( AbstractNeuron srcNeuron, Neighbor toNeuron, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		return link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
	}
	
	public static int create( AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		return link( srcNeuron, toNeuron, distance, baseSpeed, near, plasticityCfg );
	}

	public static int create( Neighbor srcNeuron, AbstractNeuron toNeuron, long baseSpeed, boolean near, SynapsePlasticityConfig plasticityCfg ) {
		return link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
	}
	
	public static int create( AbstractNeuron srcNeuron, Collection<Neighbor> toNeurons, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor toNeuron : toNeurons) {
			link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
		}
		return toNeurons.size();
	}

	public static int create( Collection<Neighbor> srcNeurons, AbstractNeuron toNeuron, long baseSpeed, boolean near,SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor srcNeuron : srcNeurons) {
			link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), baseSpeed, near, plasticityCfg );
		}
		return srcNeurons.size();
	}

	private static final int link(AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, long baseSpeed, boolean near, SynapsePlasticityConfig plasticityCfg) {
		Synapse s = new Synapse( 
			srcNeuron, 
			toNeuron, 
			distance, 
			baseSpeed,
			srcNeuron.isInhibitor() 
				?new InhibitorySynapticPlasticityRule( plasticityCfg.inhibitory())
				:new ExcitatorySynapticPlasticityRule( plasticityCfg.excitatory())
		);
		if (srcNeuron.getLayerId()==toNeuron.getLayerId()) {
			srcNeuron.addSynapse( s, false, near); 
			toNeuron.addSynapse( s, true, near);
		} else {
			srcNeuron.addSynapse( s, false, srcNeuron.getLayerId()); 
			toNeuron.addSynapse( s, true, toNeuron.getLayerId());
		}
		return 1;
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
