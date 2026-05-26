package com.cortex.base;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;

import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.layers.Layer.Neighbor;
import com.cortex.commons.IPlasticSynapse;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Pair;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseUpdatedData;
import com.cortex.globals.GlobalContext;

public final class Synapse implements IPlasticSynapse {

    public static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = n -> true;
    public static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
    public static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = AbstractNeuron::isInhibitor;

    private final AbstractNeuron pre;
    private final AbstractNeuron post;
    private final IPlasticityRule plasticityRule;
	private final float length;
	
	private final Queue<Spike> spikes = new ConcurrentLinkedQueue<>();
	
	public Synapse(AbstractNeuron pre, AbstractNeuron post, float length, IPlasticityRule plasticityRule) {
		super();
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.plasticityRule = plasticityRule;
	}
	
	public static int create( AbstractNeuron srcNeuron, Collection<Neighbor> toNeurons, SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor toNeuron : toNeurons) {
			link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), plasticityCfg );
		}
		return toNeurons.size();
	}
	
	public static int create( Collection<Neighbor> srcNeurons, AbstractNeuron toNeuron, SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor srcNeuron : srcNeurons) {
			link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), plasticityCfg );
		}
		return srcNeurons.size();
	}
	
	public static void create( AbstractNeuron srcNeuron, Pair<AbstractNeuron, Float> toNeuron, SynapsePlasticityConfig plasticityCfg) {
		link( srcNeuron, toNeuron.left(), toNeuron.right(), plasticityCfg );
	}
	
	private static void link(AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, SynapsePlasticityConfig plasticityCfg) {
		Synapse s = new Synapse( 
			srcNeuron, 
			toNeuron, 
			distance, 
			srcNeuron.isInhibitor() 
				?new InhibitorySynapticPlasticity( plasticityCfg.inhibitory())
				:new ExcitatorySynapticPlasticity( plasticityCfg.excitatory())
		);
		toNeuron.addIncomingSynapse( s );
		srcNeuron.addOutgoingSynapse( s ); 
	}
	
	public float getLength() {
		return length;
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
	public void forEachSpike(Function<Spike, Spike> consumer) {
        while (true) {
            Spike head = spikes.peek();
            if (head == null) break;
            Spike result;
            try {
                result = consumer.apply(head);
            } catch (RuntimeException ex) {
                // Protect the processing loop from consumer exceptions.
                // Log and break to avoid busy-looping on a problematic consumer.
                ex.printStackTrace();
                break;
            }
            if (result == head) {
                // keep head and stop processing further (still in flight)
                break;
            } else {
                // remove head and continue to next
                spikes.poll();
            }
        }
    }
	
	public void addSpike(Spike spike) throws InterruptedException {
		this.spikes.add(spike);
	}
	
	public boolean isEmpty() {
		return spikes.isEmpty();
	}
	
	// IPlasticSynapse
	@Override
	public void onPreSpike(long t) {
	//	if (!pre.isInhibitor()) {
            if (this.plasticityRule.onPreSpike(t)) {
                GlobalContext.addRecentlyActiveSynapses(this);
            }
     //   }
    }
	
	@Override
    public void onPostSpike(long postSpikeTime, long now) {
		if ( this.plasticityRule.onPostSpike(this, postSpikeTime, now) ) {
			GlobalContext.addRecentlyActiveSynapses(this);
		}
		this.plasticityRule.updateDelay(postSpikeTime);
    }
	
	@Override
    public void applyReward(float deltaW, long now, float neuromodulator) {
		this.plasticityRule.applyReward(deltaW, now, neuromodulator);
	}
	
	@Override
    public void update(long t) {
		float oldValue = this.plasticityRule.getWeight();
		this.plasticityRule.update(t, this);
		// pre.synapseUpdated( t, this, oldValue, this.plasticityRule.getWeight() );
		EventBus.fire(EventType.SYNAPSE_UPDATED, t, this, new SynapseUpdatedData(oldValue, this.plasticityRule.getWeight()));
		
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
