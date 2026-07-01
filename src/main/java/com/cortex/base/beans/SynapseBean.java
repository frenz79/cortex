package com.cortex.base.beans;

import java.util.Objects;

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

	@Override
	public int hashCode() {
		return Objects.hash(sourceNeuronId, targetNeuronId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SynapseBean other = (SynapseBean) obj;
		return sourceNeuronId == other.sourceNeuronId && targetNeuronId == other.targetNeuronId;
	}
}

