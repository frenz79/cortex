package com.cortex.base.beans;

public final class SynapseBean {
    public final int id;              // assegnato dal builder
    public final int sourceNeuronId;
    public final int targetNeuronId;
    public final float weight;
    public final int delayNanos;
    public final BranchBean branch;   // opzionale

    public SynapseBean(int id, int source, int target, float weight, int delayNanos, BranchBean branch) {
        this.id = id;
        this.sourceNeuronId = source;
        this.targetNeuronId = target;
        this.weight = weight;
        this.delayNanos = delayNanos;
        this.branch = branch;
    }
}

