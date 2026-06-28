package com.cortex.brain;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.soa.LateralInhibitionLayerSoA;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.soa.SpikeBufferSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.soa.logic.CombinedLateralInhibitionLogic;
import com.cortex.base.soa.logic.DendriticCompetitionLogic;
import com.cortex.base.soa.logic.SpikeRingBufferLogic;
import com.cortex.base.soa.logic.SynapseBranchLogic;
import com.cortex.base.soa.logic.SynapseLogic;

public class HemisphereContext {
	private final Logger logger = LogManager.getLogger(this.getClass());
	
	public static final int MAX_HEMISPHERES = 8;
	private static final HemisphereContext[] hemispheres = new HemisphereContext[MAX_HEMISPHERES];
	
	public final NeuronSoA neuronSoA;
	public final SynapseSoA synapseSoA;
	public final SynapseBranchSoA synapseBranchSoA;
	public final SynapseTopologySoA synapseTopologySoA;
	public final PlasticitySoA plasticitySoA;
	public final SpikeBufferSoA spikeBuffer;
	public final LateralInhibitionLayerSoA lateralInhibitionLayerSoA;
	
	public final SynapseLogic synapseLogic;
	public final SynapseBranchLogic synapseBranchLogic;
	public final CombinedLateralInhibitionLogic combinedLateralInhibitionLogic;
	public final DendriticCompetitionLogic dendriticCompetitionLogic;
	public final SpikeRingBufferLogic spikeBufferLogic;
	 
	public HemisphereContext(int hemisphereId, int neuronsCount, int synapsesCount, int branchesCount, int hemisphereCount) throws Exception {
		logger.info("Building SoA modules for hemisphereId:{}", hemisphereId);
		neuronSoA = new NeuronSoA(neuronsCount);
		synapseSoA = new SynapseSoA(synapsesCount);
		synapseBranchSoA = new SynapseBranchSoA(branchesCount);
		synapseTopologySoA = new SynapseTopologySoA(branchesCount, synapsesCount);
		plasticitySoA = new PlasticitySoA();
		lateralInhibitionLayerSoA = new LateralInhibitionLayerSoA(hemisphereCount);
		
		logger.info("Building Logic modules for hemisphereId:{}", hemisphereId);
		// TODO
		synapseLogic = new SynapseLogic(branchesCount, null, null);
		spikeBuffer = new SpikeBufferSoA(4096);
		spikeBufferLogic = new SpikeRingBufferLogic(hemisphereId, 4096, 512, 500);
		
		combinedLateralInhibitionLogic = new CombinedLateralInhibitionLogic(
			neuronSoA,
			synapseBranchSoA,
			null, // LateralInhibitionLayerSoA
			null  // SpatialHashSoA
		);
		
		synapseBranchLogic = new SynapseBranchLogic(hemisphereId);
		dendriticCompetitionLogic = new DendriticCompetitionLogic(hemisphereId);
	}

	public static HemisphereContext get(int hemisphereId) {
		return hemispheres[hemisphereId];
	}

	public static void register(int hemisphereId, HemisphereContext ctx) {
		hemispheres[hemisphereId] = ctx;
	}
}
