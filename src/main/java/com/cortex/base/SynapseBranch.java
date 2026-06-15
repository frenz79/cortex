package com.cortex.base;

public final class SynapseBranch {
	
	public static enum BranchType {
	    NEAR,
	    FAR,
	    LAYER_FEEDFORWARD,
	    LAYER_FEEDBACK,
	    EXTERNAL // Sensors, actuators, classifiers..etc
	}
	
	public final Synapse[] synapses;
	public final boolean incoming;
	public final BranchType type;
	
	public SynapseBranch(Synapse[] synapses, boolean incoming, BranchType type) {
		super();
		this.synapses = synapses;
		this.incoming = incoming;
		this.type = type;
	}

	// Branch dynamic state
	public float branchPotential;      // Weighted input sum
	public float branchActivity;       // Recent activity (for decay)
	public float inhibition;           // Lateral inhibition level
	public float gain = 1.0f;          // Branch modulator
	public boolean active = false;	   // A synapse has spikes to process

	public boolean isActive() {
		return active;
	}
	
	@Override
	public String toString() {
		return "SynapseBranch [incoming=" + incoming + ", type=" + type + ", branchPotential=" + branchPotential
				+ ", branchActivity=" + branchActivity + ", inhibition=" + inhibition + ", gain=" + gain + "]";
	}
}