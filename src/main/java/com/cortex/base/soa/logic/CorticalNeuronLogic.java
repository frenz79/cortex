package com.cortex.base.soa.logic;

import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.dendritic_competition.IDendriticCompetitionStrategy;
import com.cortex.base.utils.Maths;
import com.cortex.brain.CorticalNeuronsConfig;

public final class CorticalNeuronLogic extends AbstractNeuronLogic {

    private final CorticalNeuronsConfig config;

    public CorticalNeuronLogic(int hemisphereId, CorticalNeuronsConfig config) {
        super(hemisphereId);
        this.config = config;
    }

    @Override
    public boolean process(long now, int neuronIndex) {
        long lastProcess = neuronSoA.lastProcessTime[neuronIndex];
        long deltaTimeNanos = now - lastProcess;

        boolean stayActive = false;
        float somaPotential = 0f;

        int[] incomingBranches = neuronSoA.incomingBranchIndices[neuronIndex];

        // --- 1. Process incoming branches ---
        for (int b : incomingBranches) {

            int start = synBranchSoA.synapseStart[b];
            int count = synBranchSoA.synapseCount[b];

            synBranchSoA.branchPotential[b] = 0f;

            boolean branchStayActive = false;

            for (int i = start; i < start + count; i++) {

                int synId = synTopologySoA.synapseIndex[i];

                // Check if synapse has spikes in ring buffer
                if (!synapseSoA.hasSpikes[synId]) continue;

                stayActive = true;
                branchStayActive = true;

                // Accumulate spikes into branch potential
                float w = synapseSoA.weight[synId];
                float amp = synapseSoA.lastSpikeAmplitude[synId];

                synBranchSoA.branchPotential[b] += w * amp;

                // Update synapse plasticity
                synapseLogic.update(synId, now);
            }

            if (branchStayActive)
            	synBranchSoA.setActive(b);
            else 
            	synBranchSoA.clearActive(b);

            // Low-pass filter branch activity
            synBranchLogic.updateBranchActivity(b, now, config);
        }

        // --- 2. Dendritic competition ---
        if (now - neuronSoA.lastDendriticCompetitionTime[neuronIndex] >
            config.DENDRITIC_COMPETITION_INTERVAL_NANOS) {

            for (BranchType type : config.DENDIRITIC_COMPETITION_STRATEGIES.keySet()) {

                int[] group = neuronSoA.branchGroups[neuronIndex][type.ordinal()];
                if (group.length < 2) continue;

                IDendriticCompetitionStrategy strategy =
                    config.DENDIRITIC_COMPETITION_STRATEGIES.get(type);

                strategy.updateCompetition(group, synBranchSoA, deltaTimeNanos);
            }

            neuronSoA.lastDendriticCompetitionTime[neuronIndex] = now;
        }

        // --- 3. Compute soma potential ---
        for (int b : incomingBranches) {

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

