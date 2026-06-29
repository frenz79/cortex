package com.cortex.base.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.cortex.base.utils.Point3f;

public final class NeuronBean {

    public final int id;
    public final int layerId;
    public final int hemisphereId;

    public Point3f position;
    public boolean inhibitor;
	
    // Key:branchType
    public final Map<Integer, CopyOnWriteArrayList<BranchBean>> incomingBranches = new ConcurrentHashMap<>();
    public final Map<Integer, CopyOnWriteArrayList<BranchBean>> outgoingBranches = new ConcurrentHashMap<>();

    private final AtomicInteger incomingSynapses = new AtomicInteger();
    private final AtomicInteger outgoingSynapses = new AtomicInteger();
    
    // Key: srd.id + "->" + dst.id
    private static final long createKey(int src, int dst) {
    	return (((long)src) << 32) | dst;
    }
    
    private final Map<Long,SynapseBean> allSynapses = new ConcurrentHashMap<>();
    
    public NeuronBean(int id, int layerId, int hemisphereId, boolean inhibitor, Point3f position) {
        this.id = id;
        this.layerId = layerId;
        this.hemisphereId = hemisphereId;
        this.position = position;
        this.inhibitor = inhibitor;
    }

	public void addOutgoingSynapse(SynapseBean s, int branchType) {
		addToBranch(outgoingBranches, s, branchType, false);
	}

	public void addIncomingSynapse(SynapseBean s, int branchType) {
		addToBranch(incomingBranches, s, branchType, true);
	}
	
	public boolean isAlreadyConnectedTo( NeuronBean n ) {
		return allSynapses.containsKey(createKey(this.id, n.id)) 
			|| allSynapses.containsKey(createKey(n.id, this.id));
	}
	
	private void addToBranch( Map<Integer, CopyOnWriteArrayList<BranchBean>> map, SynapseBean s, int branchType, boolean incoming) {
		map.compute( branchType, (k,v) -> {
			if (v==null) {
				v = new CopyOnWriteArrayList<>();
				v.add(new BranchBean( branchType ));
			}
			BranchBean last = v.getLast();
			if (last.size()>=16/*TODO get from config*/) {
				last = new BranchBean( branchType );
				v.add(last);
			}
			if (incoming)
				incomingSynapses.incrementAndGet();
			else
				outgoingSynapses.incrementAndGet();
			s.setBranch(last);
			last.addSynapse(s);
			
			allSynapses.put(createKey(s.sourceNeuronId, s.targetNeuronId), s);
			return v;
		});
	}
	
	public int getId() {
		return id;
	}

	public int getLayerId() {
		return layerId;
	}
	
	public Point3f getPosition() {
		return position;
	}

	public boolean isInhibitor() {
		return inhibitor;
	}

	public void setInhibitor(boolean inhibitor) {
		this.inhibitor = inhibitor;
	}

	public void setPosition(Point3f position) {
		this.position = position;
	}
	
	public boolean hasIncoming() {
		return !incomingBranches.isEmpty();
	}
	
	public boolean hasOutgoing() {
		return !outgoingBranches.isEmpty();
	}

	public int getIncomingSynapses() {
		return incomingSynapses.get();
	}

	public int getOutgoingSynapses() {
		return outgoingSynapses.get();
	}
}
