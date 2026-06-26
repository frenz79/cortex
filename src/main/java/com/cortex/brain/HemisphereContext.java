package com.cortex.brain;

import com.cortex.base.soa.NeuronStateSoA;
import com.cortex.base.soa.SynapseBranchStateSoA;
import com.cortex.base.soa.SynapseStateSoA;
import com.cortex.base.soa.SynapseTopologySoA;

public class HemisphereContext {

	private static final HemisphereContext[] hemispheres = new HemisphereContext[MAX_HEMISPHERES];

	public final NeuronStateSoA neuronState;
	public final SynapseStateSoA synState;
	public final SynapseBranchStateSoA branchState;
	public final SynapseTopologySoA synTopology;

	public HemisphereContext(int neurons, int synapses, int branches) {
		neuronState = new NeuronStateSoA(neurons);
		synState = new SynapseStateSoA(synapses);
		branchState = new SynapseBranchStateSoA(branches);
		synTopology = new SynapseTopologySoA(synapses);
	}

	public static HemisphereContext get(int hemisphereId) {
		return hemispheres[hemisphereId];
	}

	public static void register(int hemisphereId, HemisphereContext ctx) {
		hemispheres[hemisphereId] = ctx;
	}
}
