package com.cortex.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Function;

import com.cortex.commons.IPlasticSynapse;
import com.cortex.commons.IPlasticityRule;
import com.cortex.commons.Pair;

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

	public static enum PLASTICITY_RULE {
		EXICITATORY,
		INHIBITORY,
		NONE
	}
	
	private final boolean immutable;
    private final AbstractNeuron pre;
    private final AbstractNeuron post;
    private final IPlasticityRule plasticityRule;
	private final float length;
	
	private final List<Spike> spikes = new ArrayList<>();
	
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(false);
	private static final ReadLock rLock = lock.readLock();
	private static final WriteLock wLock = lock.writeLock();
	
	public Synapse(AbstractNeuron pre, AbstractNeuron post, float length, PLASTICITY_RULE plsticity) {
		super();
		this.pre = pre;
		this.post = post;
		this.length = length;
		this.immutable = PLASTICITY_RULE.NONE.equals(plsticity);
		switch(plsticity) {
		case EXICITATORY:
			this.plasticityRule = new ExcitatorySynapticPlasticity(0.2f, 5f);
			break;
		case INHIBITORY:
			this.plasticityRule = new InhibitorySynapticPlasticity(0.2f);
			break;
		default:
			this.plasticityRule = NoSynapticPlasticity.SINGLETON_INSTANCE;
			break;		
		}
	}
	
	public static int create( AbstractNeuron srcNeuron, List<Pair<AbstractNeuron, Float>> toNeurons, PLASTICITY_RULE plsticity ) {
		for (Pair<AbstractNeuron, Float> toNeuron : toNeurons) {
			link( srcNeuron, toNeuron.left(), toNeuron.right() , plsticity );
		}
		return toNeurons.size();
	}
	
	public static int create( List<Pair<AbstractNeuron, Float>> srcNeurons, AbstractNeuron toNeuron, PLASTICITY_RULE plsticity ) {
		for (Pair<AbstractNeuron, Float> srcNeuron : srcNeurons) {
			link( srcNeuron.left(), toNeuron, srcNeuron.right() , plsticity );
		}
		return srcNeurons.size();
	}
	
	public static void create( AbstractNeuron srcNeuron, Pair<AbstractNeuron, Float> toNeuron, PLASTICITY_RULE plsticity) {
		link( srcNeuron, toNeuron.left(), toNeuron.right() , plsticity );
	}
	
	private static void link(AbstractNeuron srcNeuron, AbstractNeuron toNeuron, float distance, PLASTICITY_RULE plsticity) {
		Synapse s = new Synapse( srcNeuron, toNeuron, distance, plsticity );
		toNeuron.addIncomingSynapse( s );
		srcNeuron.addOutgoingSynapse( s ); 
	}
	
	public float getLength() {
		return length;
	}
	
	public void forEachSpike( Function<Spike, Boolean> consumer ) {
		List<Integer> toBeRemoved = new ArrayList<>(spikes.size());
		rLock.lock();
		try {			
			for ( int i=0; i<spikes.size(); i++ ) {
				if (!consumer.apply(spikes.get(i))) {
					toBeRemoved.add(i);
				}
			}
		} finally {
			rLock.unlock();
		}
		wLock.lock();
		try {
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
	
	public void addSpike(Spike spike) {
		wLock.lock();
		try {
			this.spikes.add(spike);
		} finally {
			wLock.unlock();
		}
	}
	
	public AbstractNeuron getTarget() {
		return post;
	}
	
	public boolean isImmutable() {
		return immutable;
	}

	public boolean isEmpty() {
		return spikes.isEmpty();
	}
	
	// IPlasticSynapse
	@Override
	public void onPreSpike(long t) {
		if (pre.isInhibitor()) {
			
		} else {
			this.plasticityRule.onPreSpike(t);
		}
    }
	@Override
    public void onPostSpike(long t, long now) {
		this.plasticityRule.onPostSpike(this, t, now);
    }
	@Override
    public void applyReward(float r, long t) {
	//	this.plasticityRule.applyReward(r, t);
	//	this.plasticityRule.updateDelay(r);
    }
	@Override
    public void update(long t) {
	//	this.plasticityRule.update(t);
    }
	@Override
    public float getWeight() {
		return this.plasticityRule.getWeight();
    }
}
