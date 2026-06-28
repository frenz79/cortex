package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.soa.SynapseTopologySoA;
import com.cortex.base.utils.Maths;
import com.cortex.brain.CorticalNeuronsConfig;

public final class SynapseBranchLogic {

    private final SynapseBranchSoA branchSoA;
    private final SynapseSoA synapseSoA;
    private final SynapseTopologySoA synTopologySoA;
    private final SpikeRingBufferLogic spikeBufferLogic;
    
    public SynapseBranchLogic(
    	int hemisphereId,
    	SynapseBranchSoA branchSoA,
    	SynapseSoA synapseSoA,
    	SynapseTopologySoA synTopologySoA,
    	SpikeRingBufferLogic spikeBufferLogic
     ) {
    	Objects.nonNull(branchSoA);
    	Objects.nonNull(synapseSoA);
    	Objects.nonNull(synTopologySoA);
    	Objects.nonNull(spikeBufferLogic);
    	
        this.branchSoA = branchSoA;
        this.synapseSoA = synapseSoA;
        this.synTopologySoA = synTopologySoA;
        this.spikeBufferLogic = spikeBufferLogic;
    }

    // --- Update branch activity + gain (homeostasis) ---
    public void updateBranch(int branchIndex, long now, CorticalNeuronsConfig config) {

        long dt = now - branchSoA.lastProcessTime[branchIndex];
        if (dt <= 5_000_000L) return;

        // Low-pass filter activity
        float decay = Maths.exp(-(float) dt / config.BRANCH_GAIN_TAU_NANOS);
        branchSoA.branchActivity[branchIndex] =
                branchSoA.branchActivity[branchIndex] * decay +
                branchSoA.branchPotential[branchIndex];

        // Gain homeostasis
        float alpha = Maths.clamp(
                (float) dt / config.BRANCH_GAIN_TAU_NANOS,
                0.0f, 1.0f);

        float error = branchSoA.branchActivity[branchIndex] - config.BRANCH_TARGET_ACTIVITY;
        float newGain = branchSoA.gain[branchIndex] + alpha * error;

        branchSoA.gain[branchIndex] = Maths.clamp(
                newGain,
                config.BRANCH_GAIN_MIN,
                config.BRANCH_GAIN_MAX);

        branchSoA.lastProcessTime[branchIndex] = now;
    }

    // --- Compute branch potential from synapses ---
    public boolean accumulateBranchPotential(long now, int branchIndex) {

        int start = branchSoA.synapseStart[branchIndex];
        int count = branchSoA.synapseCount[branchIndex];

        float potential = 0f;
        boolean active = false;

        for (int i = start; i < start + count; i++) {
            int synId = synTopologySoA.synapseIndex[i];

            if (!spikeBufferLogic.hasSpikeForSynapse(synId)) continue;

            active = true;
            float w = synapseSoA.weight[synId];
            float amp = spikeBufferLogic.getLastSpikeAmplitudeForSynapse(synId);
            potential += w * amp;
        }

        branchSoA.branchPotential[branchIndex] = potential;
        branchSoA.setActive(branchIndex, active);

        return active;
    }

    // --- Inhibition decay ---
    public void updateInhibition(int branchIndex, long dt, CorticalNeuronsConfig config) {

        double windows = (double) dt / config.INHIBITION_TAU_NANOS;

        branchSoA.inhibition[branchIndex] *=
                Maths.pow(config.INHIBITION_DECAY_PER_WINDOW, windows);

        if (branchSoA.inhibition[branchIndex] < 1e-9f)
            branchSoA.inhibition[branchIndex] = 0f;
    }

    // --- Dendritic competition support ---
    public float getBranchActivity(int branchIndex) {
        return branchSoA.branchActivity[branchIndex];
    }

    public float getBranchPotential(int branchIndex) {
        return branchSoA.branchPotential[branchIndex];
    }

    public float getGain(int branchIndex) {
        return branchSoA.gain[branchIndex];
    }

    public void setGain(int branchIndex, float value) {
        branchSoA.gain[branchIndex] = value;
    }
}

