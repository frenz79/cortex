package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.LateralInhibitionLayerSoA;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.soa.SpatialHashSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.utils.Maths;

public final class CombinedLateralInhibitionLogic {

    private final NeuronSoA neuronSoA;
    private final SynapseBranchSoA synBranchSoA;
    private final LateralInhibitionLayerSoA lateralInhibLayerSoA;
    private final SpatialHashSoA spatialHashSoA;

    public CombinedLateralInhibitionLogic(
        NeuronSoA neuronSoA,
        SynapseBranchSoA synBranchSoA,
        LateralInhibitionLayerSoA lateralInhibLayerSoA,
        SpatialHashSoA spatialHashSoA
    ) {
        Objects.nonNull(neuronSoA);
        Objects.nonNull(synBranchSoA);
        Objects.nonNull(lateralInhibLayerSoA);
        Objects.nonNull(spatialHashSoA);
        
        this.neuronSoA = neuronSoA;
        this.synBranchSoA = synBranchSoA;
        this.lateralInhibLayerSoA = lateralInhibLayerSoA;
        this.spatialHashSoA = spatialHashSoA;
    }

    /**
     * Apply both topographic and synaptical lateral inhibition
     * to all branches of neurons influenced by neuronId.
     */
    public void process(long now, int neuronId) {

        // Layer-specific parameters
        int layer = neuronSoA.getLayerId(neuronId);

        float topoStrength = lateralInhibLayerSoA.topoStrength[layer];
        float topoWeight   = lateralInhibLayerSoA.topoWeight[layer];
        float topoRadius   = lateralInhibLayerSoA.topoRadius[layer];

        float synStrength  = lateralInhibLayerSoA.synStrength[layer];
        float synWeight    = lateralInhibLayerSoA.synWeight[layer];
        float synScale     = lateralInhibLayerSoA.synActivityScale[layer];

        // Early exit if both strategies are disabled
        if (topoStrength <= 0f && synStrength <= 0f)
            return;

        // Position of the firing neuron
        float x0 = neuronSoA.posX[neuronId];
        float y0 = neuronSoA.posY[neuronId];
        float z0 = neuronSoA.posZ[neuronId];

        // Get neighbors from spatial hash
        int[] neighbors = spatialHashSoA.getNeighbors(layer, neuronId);

        for (int nId : neighbors) {

            // Skip self and inhibitory neurons
            if (nId == neuronId) continue;
            if (neuronSoA.isInhibitory(nId)) continue;

            // Compute distance
            float dx = neuronSoA.posX[nId] - x0;
            float dy = neuronSoA.posY[nId] - y0;
            float dz = neuronSoA.posZ[nId] - z0;

            float dist = (float)Maths.sqrt(dx*dx + dy*dy + dz*dz);

            // --- Topographic LI ---
            float topoNorm = 1.0f - dist / topoRadius;
            float topoInhib = (topoNorm > 0f)
                ? topoStrength * topoWeight * topoNorm
                : 0f;

            // --- Synaptical LI ---
            float synActivity = synBranchSoA.branchActivity[nId];
            float synInhib = synStrength * synWeight * synActivity * synScale;

            // Combined inhibition
            float inhib = topoInhib + synInhib;
            if (inhib <= 0f) continue;

            // Apply inhibition to all incoming branches of the target neuron
            int first = synBranchSoA.synapseStart[nId];
            int count = synBranchSoA.synapseCount[nId];

            for (int i = 0; i < count; i++) {
                int bId = first + i;

                // Skip external branches
                if (synBranchSoA.isExternalBranchType(bId))
                    continue;

                synBranchSoA.inhibition[bId] += inhib;
            }
        }
    }
}

