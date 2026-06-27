package com.cortex.brain;

import com.cortex.base.soa.LateralInhibitionLayerSoA;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.soa.SpikeRingBufferSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.soa.logic.CombinedLateralInhibitionLogic;
import com.cortex.base.soa.logic.SynapseLogic;

public class HemisphereContext {
	public static final int MAX_HEMISPHERES = 8;
	private static final HemisphereContext[] hemispheres = new HemisphereContext[MAX_HEMISPHERES];
	
	public final NeuronSoA neuronSoA;
	public final SynapseSoA synapseSoA;
	public final SynapseBranchSoA synapseBranchSoA;
	public final SynapseTopologySoA synapseTopologySoA;
	public final PlasticitySoA plasticitySoA;
	public final SynapseLogic synapseLogic;
	public final SpikeRingBufferSoA spikeBuffer;
	public final LateralInhibitionLayerSoA lateralInhibitionLayerSoA;
	public final CombinedLateralInhibitionLogic combinedLateralInhibitionLogic;
	
	public HemisphereContext(int neuronsCount, int synapsesCount, int branchesCount, int hemisphereCount) {
		neuronSoA = new NeuronSoA(neuronsCount);
		synapseSoA = new SynapseSoA(synapsesCount);
		synapseBranchSoA = new SynapseBranchSoA(branchesCount);
		synapseTopologySoA = new SynapseTopologySoA(branchesCount, synapsesCount);
		plasticitySoA = new PlasticitySoA();
		// TODO
		synapseLogic = new SynapseLogic(branchesCount, null, null);
		spikeBuffer = new SpikeRingBufferSoA(4096, 512, 500);
		lateralInhibitionLayerSoA = new LateralInhibitionLayerSoA(hemisphereCount);
		combinedLateralInhibitionLogic = new CombinedLateralInhibitionLogic(
			neuronSoA,
			synapseBranchSoA,
			null, // LateralInhibitionLayerSoA
			null  // SpatialHashSoA
		);
	}

	public static HemisphereContext get(int hemisphereId) {
		return hemispheres[hemisphereId];
	}

	public static void register(int hemisphereId, HemisphereContext ctx) {
		hemispheres[hemisphereId] = ctx;
	}
}
