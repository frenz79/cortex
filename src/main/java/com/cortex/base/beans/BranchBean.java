package com.cortex.base.beans;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BranchBean {

    public final int type; // BranchTypeCode

    // thread-safe
    private final CopyOnWriteArrayList<SynapseBean> synapses = new CopyOnWriteArrayList<>();

    public BranchBean(int type) {
        this.type = type;
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
