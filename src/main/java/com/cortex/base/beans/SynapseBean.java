package com.cortex.base.beans;

public final class SynapseBean {
    public final int sourceNeuronId;
    public final int targetNeuronId;
    public final float length;
    public BranchBean branch;

    public SynapseBean(int source, int target, float distance) {
        this.sourceNeuronId = source;
        this.targetNeuronId = target;
        this.length = distance;
    }

	public BranchBean getBranch() {
		return branch;
	}

	public void setBranch(BranchBean branch) {
		this.branch = branch;
	}
}

