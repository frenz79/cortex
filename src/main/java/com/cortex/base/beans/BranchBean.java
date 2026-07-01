package com.cortex.base.beans;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class BranchBean {

	public static final AtomicInteger idGen = new AtomicInteger();
	
	public final int id;
    public final int type; // BranchTypeCode
    public final boolean incoming;
    
    // thread-safe
    private final CopyOnWriteArrayList<SynapseBean> synapses = new CopyOnWriteArrayList<>();

    public BranchBean( int type, boolean incoming) {
    	this.id = idGen.getAndIncrement();
        this.type = type;
        this.incoming = incoming;
    }

    public void addSynapse(SynapseBean s) {
        synapses.add(s); // thread-safe append
    }

    public int size() {
        return synapses.size(); // thread-safe read
    }

    public List<SynapseBean> getSynapses() {
        return synapses; // safe to expose (immutable snapshot semantics)
    }
}
