package com.cortex.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Function;

import com.cortex.commons.IPlasticSynapse;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Pair;
import com.cortex.layer.Layer.Neighbor;
import com.cortex.layer.SynapsePlasticityConfig;

/**
 
  🧪 Policy consigliate (importantissime)
	Tipo sinapsi		STDP	Reward	Delay	Decay
	Retina → L1			❌		❌		❌		❌
	L1 → L4				✅		❌		❌		✅
	L4 → L_out			✅		✅		❌		✅
	Feedback L_out → L4	❌		✅		❌		❌
	Inibitori locali	❌		❌		❌		❌
  
*/
public final class Synapse implements IPlasticSynapse {

    private final AbstractNeuron pre;
    private final AbstractNeuron post;
    private final IPlasticityRule plasticityRule;
	private final float length;
	
	private final List<Spike> spikes = new ArrayList<>();
	
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
	private static final ReadLock rLock = lock.readLock();
	private static final WriteLock wLock = lock.writeLock();
	
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
				?plasticityCfg.inhibitorySynapticPlasticity()
				:plasticityCfg.excitatorySynapticPlasticity()
		);
		toNeuron.addIncomingSynapse( s );
		srcNeuron.addOutgoingSynapse( s ); 
	}
	
	public float getLength() {
		return length;
	}
	
	public void forEachSpike( Function<Spike, Boolean> consumer ) throws InterruptedException {
		wLock.tryLock(1, TimeUnit.SECONDS);
		try {
			List<Integer> toBeRemoved = new ArrayList<>(spikes.size());
		
			for ( int i=0; i<spikes.size(); i++ ) {
				if (!consumer.apply(spikes.get(i))) {
					toBeRemoved.add(i);
				}
			}
			if (!toBeRemoved.isEmpty()) {
				// inverse iteration
				for ( int i=toBeRemoved.size()-1; i>=0 ; i-- ) {
					spikes.remove(i);
				}
			}
		} finally {
			wLock.unlock();
		}
	}
	
	public void addSpike(Spike spike) throws InterruptedException {
		wLock.tryLock(1, TimeUnit.SECONDS);
		try {
			this.spikes.add(spike);
		} finally {
			wLock.unlock();
		}
	}
	
	public AbstractNeuron getTarget() {
		return post;
	}
	
	public boolean isEmpty() {
		return spikes.isEmpty();
	}
	
	// IPlasticSynapse
	@Override
	public void onPreSpike(long t) {
		if (pre.isInhibitor()) {
			// NOP
		} else {
			this.plasticityRule.onPreSpike(t);
		}
    }
	@Override
    public void onPostSpike(long t, long now) {
		this.plasticityRule.onPostSpike(this, t, now);
		this.plasticityRule.updateDelay(t);
    }
	@Override
    public void applyReward(float r, long t) {
		this.plasticityRule.applyReward(r, t);
	//	this.plasticityRule.updateDelay(r);
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
}
