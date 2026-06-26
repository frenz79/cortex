package com.cortex.base;

import com.cortex.base.utils.Maths;
import com.cortex.brain.CorticalNeuronsConfig;

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
	public long lastProcessTime = System.nanoTime();

	public boolean isActive() {
		return active;
	}

	public int size() {
		return synapses.length;
	}

	public void update(long now, CorticalNeuronsConfig config) {
		long dtBranch = now - lastProcessTime;
		if ( dtBranch > 5_000_000l ) {
			float decay = Maths.exp(-(float)dtBranch / config.BRANCH_GAIN_TAU_NANOS);
			branchActivity = branchActivity * decay +  branchPotential;

			// Update branch gain (homeostatic regulation)
			float alpha = Maths.clamp(
					(float) dtBranch / (float) config.BRANCH_GAIN_TAU_NANOS,
					0.0f, 1.0f);

			float error = branchActivity - config.BRANCH_TARGET_ACTIVITY;
			gain += alpha * error;
			gain = Maths.clamp(
					gain,
					config.BRANCH_GAIN_MIN,
					config.BRANCH_GAIN_MAX);
			lastProcessTime = now;
		}
	}

	public boolean hasSpikes() {
		for (Synapse synapse : synapses) {
			if (!synapse.isEmpty()) {
				return true;
			}
		}
		return false;
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