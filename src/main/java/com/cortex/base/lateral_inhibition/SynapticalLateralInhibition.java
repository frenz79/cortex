package com.cortex.base.lateral_inhibition;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.SynapseBranch;
import com.cortex.base.SynapseBranch.BranchType;

public class SynapticalLateralInhibition implements ILateralInhibitionStrategy {

	private final float LI_STRENGTH;
	
	public SynapticalLateralInhibition(float LI_STRENGTH) {
		this.LI_STRENGTH = LI_STRENGTH;
	}
	
	@Override
	public void updateInhibition(long deltaTimeNanos, AbstractNeuron n, float gain) {
		for (SynapseBranch out : n.getOutSynapseBranches()) {
		    for (Synapse s : out.synapses) {
		        AbstractNeuron target = s.getTarget();
		        for (SynapseBranch b : target.getInSynapseBranches()) {
		        	if (b.type == BranchType.EXTERNAL) continue;
		            // Inhibition proportional to sinpatic branch activity
		            float inhib = LI_STRENGTH * gain * out.branchActivity;
		            b.inhibition += inhib;
		        }
		    }
		}
	}
}
