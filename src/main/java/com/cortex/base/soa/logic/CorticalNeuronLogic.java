package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.NeuronTopologySoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.utils.Maths;
import com.cortex.brain.CorticalNeuronsConfig;

public final class CorticalNeuronLogic extends AbstractNeuronLogic {

	private final CorticalNeuronsConfig[] corticalNeuronsConfigs;

	private final SynapseBranchLogic synapseBranchLogic;
	private final DendriticCompetitionLogic dendriticCompetitionLogic;
	private final SynapseSoA synapseSoA;

	public CorticalNeuronLogic(
			CorticalNeuronsConfig[] corticalNeuronsConfigs,
			SynapseBranchLogic synapseBranchLogic,
			DendriticCompetitionLogic dendriticCompetitionLogic,

			// SoA dep
			NeuronSoA neuronSoA,
			NeuronTopologySoA neuronTopologySoA,
			SynapseSoA synapseSoA,
			SynapseBranchSoA synBranchSoA,
			SynapseTopologySoA synTopologySoA,
			// Logic dep
			SynapseLogic synapseLogic,
			SpikeRingBufferLogic spikeBufferLogic,
			CombinedLateralInhibitionLogic combinedLateralInhibitionLogic
	) {
		super( neuronSoA, neuronTopologySoA, synBranchSoA, synTopologySoA, synapseLogic, spikeBufferLogic, combinedLateralInhibitionLogic );
		this.corticalNeuronsConfigs = corticalNeuronsConfigs;

		Objects.nonNull(synapseBranchLogic);
		Objects.nonNull(dendriticCompetitionLogic);
		Objects.nonNull(neuronTopologySoA);
		Objects.nonNull(synapseSoA);

		this.synapseBranchLogic = synapseBranchLogic;
		this.dendriticCompetitionLogic = dendriticCompetitionLogic;
		this.synapseSoA = synapseSoA;
	}

	@Override
	public boolean process(long now, int neuronIndex) {

	    long lastProcess = neuronSoA.lastProcessTime[neuronIndex];
	    long deltaTimeNanos = now - lastProcess;

	    boolean stayActive = false;
	    float somaPotential = 0f;

	    // Config layer-specific
	    CorticalNeuronsConfig config =
	            corticalNeuronsConfigs[neuronSoA.getLayerId(neuronIndex)];

	    // --- 1. Process incoming branches (HPC flatten) ---
	    int inStart = neuronTopologySoA.incomingBranchStart[neuronIndex];
	    int inCount = neuronTopologySoA.incomingBranchCount[neuronIndex];

	    for (int bi = inStart; bi < inStart + inCount; bi++) {

	        int b = neuronTopologySoA.incomingBranches[bi];

	        int synStart = synBranchSoA.synapseStart[b];
	        int synCount = synBranchSoA.synapseCount[b];

	        synBranchSoA.branchPotential[b] = 0f;
	        boolean branchStayActive = false;

	        for (int si = synStart; si < synStart + synCount; si++) {

	            int synId = synTopologySoA.synapseIndex[si];

	            // Spike present?
	            if (!spikeBufferLogic.hasSpikeForSynapse(synId)) continue;

	            stayActive = true;
	            branchStayActive = true;

	            float w = synapseSoA.weight[synId];
	            float amp = spikeBufferLogic.synapseLastAmplitude[synId];

	            synBranchSoA.branchPotential[b] += w * amp;

	            // Plasticity update
	            synapseLogic.update(synId, now);
	        }

	        if (branchStayActive)
	            synBranchSoA.setActive(b);
	        else
	            synBranchSoA.clearActive(b);

	        // Low-pass filter branch activity
	        synapseBranchLogic.updateBranch(b, now, config);
	    }

	    // --- 2. Dendritic competition ---
	    dendriticCompetitionLogic.process(now, neuronIndex);

	    // --- 3. Compute soma potential ---
	    for (int bi = inStart; bi < inStart + inCount; bi++) {

	        int b = neuronTopologySoA.incomingBranches[bi];

	        double windows = (double) deltaTimeNanos / config.INHIBITION_TAU_NANOS;
	        synBranchSoA.inhibition[b] *= Maths.pow(config.INHIBITION_DECAY_PER_WINDOW, windows);

	        if (synBranchSoA.inhibition[b] < 1e-9f)
	            synBranchSoA.inhibition[b] = 0f;

	        float modulated =
	                synBranchSoA.branchPotential[b] * synBranchSoA.gain[b]
	                        - synBranchSoA.inhibition[b];

	        somaPotential += modulated;
	    }

	    // --- 4. Firing decision ---
	    boolean inRefractory =
	            (now - neuronSoA.lastSpikeTime[neuronIndex]) < config.REFRACTORY_PERIOD_NANOS;

	    if (!inRefractory && somaPotential > config.FIRING_THRESHOLD) {
	        fire(now, neuronIndex);
	    }

	    neuronSoA.lastProcessTime[neuronIndex] = now;
	    return stayActive;
	}

	@Override
	public float getRecentFiringRate(long now, int neuronIndex) {
		CorticalNeuronsConfig config = corticalNeuronsConfigs[neuronSoA.getLayerId(neuronIndex)];

		long dt = now - neuronSoA.lastRateUpdate[neuronIndex];
		if (dt <= 0) return neuronSoA.firingRate[neuronIndex];

		double windows = (double) dt / config.RATE_WINDOW_NANOS;
		neuronSoA.firingRate[neuronIndex] *= Maths.pow(config.RATE_DECAY_PER_WINDOW, windows);

		if (neuronSoA.firingRate[neuronIndex] < 1e-6f)
			neuronSoA.firingRate[neuronIndex] = 0f;

		neuronSoA.lastRateUpdate[neuronIndex] = now;
		return neuronSoA.firingRate[neuronIndex];
	}
}

