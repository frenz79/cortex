package com.cortex.base;

import com.cortex.base.soa.SynapseBranchStateSoA;
import com.cortex.base.soa.SynapseStateSoA;
import com.cortex.base.utils.Maths;
import com.cortex.brain.CorticalNeuronsConfig;
import com.cortex.brain.HemisphereContext;

public final class SynapseBranch {

	public static enum BranchType {
		NEAR,
		FAR,
		LAYER_FEEDFORWARD,
		LAYER_FEEDBACK,
		EXTERNAL // Sensors, actuators, classifiers..etc
	}

	public enum Direction { INCOMING, OUTGOING }

	private final int index;
	public final int hemisphereId;
	
	public SynapseBranch(int index, int hemisphereId, int synapseStart, int synapseCount, BranchType type, Direction direction) {
		this.index = index;
		this.hemisphereId = hemisphereId;
		SynapseBranchStateSoA branchStates = HemisphereContext.get(hemisphereId).branchState;
		branchStates.synapseStart[index] = synapseStart;
		branchStates.synapseCount[index] = synapseCount;
		branchStates.type[index] = type;
		branchStates.direction[index] = direction;
	}

	public boolean isActive() {
		return HemisphereContext.get(hemisphereId).branchState.active[index];
	}

	public int size() {
		return HemisphereContext.get(hemisphereId).branchState.synapseCount[index];
	}

	public void update(long now, CorticalNeuronsConfig config) {
		SynapseBranchStateSoA states = HemisphereContext.get(hemisphereId).branchState;
		
		long dtBranch = now - states.lastProcessTime[index];
		if ( dtBranch > 5_000_000l ) {
			float decay = Maths.exp(-(float)dtBranch / config.BRANCH_GAIN_TAU_NANOS);
			states.branchActivity[index]
					= states.branchActivity[index] * decay +  states.branchPotential[index];

			// Update branch gain (homeostatic regulation)
			float alpha = Maths.clamp(
					(float) dtBranch / (float) config.BRANCH_GAIN_TAU_NANOS,
					0.0f, 1.0f);

			float error = states.branchActivity[index] - config.BRANCH_TARGET_ACTIVITY;
			states.gain[index] += alpha * error;
			states.gain[index] = Maths.clamp(
					states.gain[index],
					config.BRANCH_GAIN_MIN,
					config.BRANCH_GAIN_MAX);
			states.lastProcessTime[index] = now;
		}
	}

	public boolean hasSpikes() {
		// return stateBuff.spikeCount[index] > 0;
		return true;
	}

	@Override
	public String toString() {
		SynapseBranchStateSoA states = HemisphereContext.get(hemisphereId).branchState;
		
		StringBuilder builder = new StringBuilder();
		builder.append("SynapseBranch [direction=");
		builder.append(states.direction[index]);
		builder.append(", type=");
		builder.append(states.type[index]);
		builder.append(", branchPotential=");
		builder.append(states.branchPotential[index]);
		builder.append(", branchActivity=");
		builder.append(states.branchActivity[index]);
		builder.append(", inhibition=");
		builder.append(states.inhibition[index]);
		builder.append(", gain=");
		builder.append(states.gain[index]);
		builder.append(", active=");
		builder.append(states.active[index]);
		builder.append("]");
		return builder.toString();
	}
}