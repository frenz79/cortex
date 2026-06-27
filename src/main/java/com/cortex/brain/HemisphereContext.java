package com.cortex.brain;

import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;

public class HemisphereContext {
	public static final int MAX_HEMISPHERES = 8;
	private static final HemisphereContext[] hemispheres = new HemisphereContext[MAX_HEMISPHERES];

	public final NeuronSoA neuronSoA;
	public final SynapseSoA synapseSoA;
	public final SynapseBranchSoA synapseBranchSoA;
	public final SynapseTopologySoA synapseTopologySoA;
	public final PlasticitySoA plasticitySoA;
	
	public HemisphereContext(int neuronsCount, int synapsesCount, int branchesCount) {
		neuronSoA = new NeuronSoA(neuronsCount);
		synapseSoA = new SynapseSoA(synapsesCount);
		synapseBranchSoA = new SynapseBranchSoA(branchesCount);
		synapseTopologySoA = new SynapseTopologySoA(branchesCount, synapsesCount);
		plasticitySoA = new PlasticitySoA();
	}

	public static HemisphereContext get(int hemisphereId) {
		return hemispheres[hemisphereId];
	}

	public static void register(int hemisphereId, HemisphereContext ctx) {
		hemispheres[hemisphereId] = ctx;
	}
}
