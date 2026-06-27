package com.cortex.base.soa.logic;

import com.cortex.base.soa.LateralInhibitionLayerSoA;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.SpatialHashSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.utils.Maths;

public final class CombinedLateralInhibitionLogic {

    private final NeuronSoA neuronState;
    private final SynapseBranchSoA branchState;
    private final LateralInhibitionLayerSoA liLayer;
    private final SpatialHashSoA spatialHash;

    public CombinedLateralInhibitionLogic(
        NeuronSoA neuronState,
        SynapseBranchSoA branchState,
        LateralInhibitionLayerSoA liLayer,
        SpatialHashSoA spatialHash
    ) {
        this.neuronState = neuronState;
        this.branchState = branchState;
        this.liLayer = liLayer;
        this.spatialHash = spatialHash;
    }

    /**
     * Apply both topographic and synaptical lateral inhibition
     * to all branches of neurons influenced by neuronId.
     */
    public void applyLateralInhibition(long now, int neuronId) {

        // Layer-specific parameters
        int layer = neuronState.getLayerId(neuronId);

        float topoStrength = liLayer.topoStrength[layer];
        float topoWeight   = liLayer.topoWeight[layer];
        float topoRadius   = liLayer.topoRadius[layer];

        float synStrength  = liLayer.synStrength[layer];
        float synWeight    = liLayer.synWeight[layer];
        float synScale     = liLayer.synActivityScale[layer];

        // Early exit if both strategies are disabled
        if (topoStrength <= 0f && synStrength <= 0f)
            return;

        // Position of the firing neuron
        float x0 = neuronState.posX[neuronId];
        float y0 = neuronState.posY[neuronId];
        float z0 = neuronState.posZ[neuronId];

        // Get neighbors from spatial hash
        int[] neighbors = spatialHash.getNeighbors(layer, neuronId);

        for (int nId : neighbors) {

            // Skip self and inhibitory neurons
            if (nId == neuronId) continue;
            if (neuronState.isInhibitory(nId)) continue;

            // Compute distance
            float dx = neuronState.posX[nId] - x0;
            float dy = neuronState.posY[nId] - y0;
            float dz = neuronState.posZ[nId] - z0;

            float dist = (float)Maths.sqrt(dx*dx + dy*dy + dz*dz);

            // --- Topographic LI ---
            float topoNorm = 1.0f - dist / topoRadius;
            float topoInhib = (topoNorm > 0f)
                ? topoStrength * topoWeight * topoNorm
                : 0f;

            // --- Synaptical LI ---
            float synActivity = branchState.branchActivity[nId];
            float synInhib = synStrength * synWeight * synActivity * synScale;

            // Combined inhibition
            float inhib = topoInhib + synInhib;
            if (inhib <= 0f) continue;

            // Apply inhibition to all incoming branches of the target neuron
            int first = branchState.synapseStart[nId];
            int count = branchState.synapseCount[nId];

            for (int i = 0; i < count; i++) {
                int bId = first + i;

                // Skip external branches
                if (branchState.isExternalBranchType(bId))
                    continue;

                branchState.inhibition[bId] += inhib;
            }
        }
    }
}

