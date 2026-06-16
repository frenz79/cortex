package com.cortex.base;

public final class SynapseBranch {
	
	public static enum BranchType {
	    NEAR,
	    FAR,
	    LAYER_FEEDFORWARD,
	    LAYER_FEEDBACK,
	    EXTERNAL // Sensors, actuators, classifiers..etc
	}
	
	public enum Direction { INCOMING, OUTGOING }
	
	public final Direction direction;
	public final Synapse[] synapses;
	public final BranchType type;
	
	public SynapseBranch(Synapse[] synapses, BranchType type, Direction direction) {
		super();
		this.synapses = synapses;
		this.direction = direction;
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
	
	public int size() {
		return synapses.length;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SynapseBranch [direction=");
		builder.append(direction);
		builder.append(", type=");
		builder.append(type);
		builder.append(", branchPotential=");
		builder.append(branchPotential);
		builder.append(", branchActivity=");
		builder.append(branchActivity);
		builder.append(", inhibition=");
		builder.append(inhibition);
		builder.append(", gain=");
		builder.append(gain);
		builder.append(", active=");
		builder.append(active);
		builder.append("]");
		return builder.toString();
	}
}