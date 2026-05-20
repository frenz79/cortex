package com.cortex.base;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import com.cortex.brain.GlobalContext;
import com.cortex.commons.IPlasticSynapse;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Pair;
import com.cortex.layer.Layer.Neighbor;
import com.cortex.layer.SynapsePlasticityConfig;

public final class Synapse implements IPlasticSynapse {

    private final Neuron pre;
    private final Neuron post;
    private final IPlasticityRule plasticityRule;
	private final float length;
	
	private final Queue<Spike> spikes = new ConcurrentLinkedQueue<>();
	
	public Synapse(Neuron pre, Neuron post, float length, IPlasticityRule plasticityRule) {
		super();
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.plasticityRule = plasticityRule;
	}
	
	public static int create( Neuron srcNeuron, Collection<Neighbor> toNeurons, SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor toNeuron : toNeurons) {
			link( srcNeuron, toNeuron.neuron(), toNeuron.getRealDistance(), plasticityCfg );
		}
		return toNeurons.size();
	}
	
	public static int create( Collection<Neighbor> srcNeurons, Neuron toNeuron, SynapsePlasticityConfig plasticityCfg ) {
		for (Neighbor srcNeuron : srcNeurons) {
			link( srcNeuron.neuron(), toNeuron, srcNeuron.getRealDistance(), plasticityCfg );
		}
		return srcNeurons.size();
	}
	
	public static void create( Neuron srcNeuron, Pair<Neuron, Float> toNeuron, SynapsePlasticityConfig plasticityCfg) {
		link( srcNeuron, toNeuron.left(), toNeuron.right(), plasticityCfg );
	}
	
	private static void link(Neuron srcNeuron, Neuron toNeuron, float distance, SynapsePlasticityConfig plasticityCfg) {
		Synapse s = new Synapse( 
			srcNeuron, 
			toNeuron, 
			distance, 
			srcNeuron.isInhibitor() 
				?plasticityCfg.inhibitorySynapticPlasticity()
				:plasticityCfg.excitatorySynapticPlasticity()
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
	
	public Neuron getTarget() {
		return post;
	}
	
	public boolean isEmpty() {
		return spikes.isEmpty();
	}
	
	// IPlasticSynapse
	@Override
	public void onPreSpike(long t) {
		if (!pre.isInhibitor()) {
            if (this.plasticityRule.onPreSpike(t)) {
                GlobalContext.addRecentlyActiveSynapses(this);
            }
        }
    }
	
	@Override
    public void onPostSpike(long t, long now) {
		if ( this.plasticityRule.onPostSpike(this, t, now) ) {
			GlobalContext.addRecentlyActiveSynapses(this);
		}
		this.plasticityRule.updateDelay(t);
    }
	@Override
    public void applyReward(float deltaW, long now, float neuromodulator) {
		this.plasticityRule.applyReward(deltaW, now, neuromodulator);
	}
	
	@Override
    public void update(long t) {
		float oldValue = this.plasticityRule.getWeight();
		this.plasticityRule.update(t);
		pre.synapseUpdated( t, this, oldValue, this.plasticityRule.getWeight() );
    }
	@Override
    public float getWeight() {
		return this.plasticityRule.getWeight();
    }

	public boolean isEligible(long now, long window) {
		return this.plasticityRule.isEligible(now,window);
	}
}
