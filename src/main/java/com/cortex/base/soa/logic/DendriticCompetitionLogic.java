package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.DendriticCompetitionParamsSoA;
import com.cortex.base.soa.DendriticTreeSoA;
import com.cortex.base.soa.SynapseBranchSoA;
import com.cortex.base.soa.constants.BranchTypeCode;

public final class DendriticCompetitionLogic {

    private final SynapseBranchSoA synapseBranchSoA;
    private final DendriticTreeSoA dendriticTreeSoA;
    private final DendriticCompetitionParamsSoA dendriticCompetitionParamsSoA;

    public DendriticCompetitionLogic(
        SynapseBranchSoA synapseBranchSoA,
        DendriticTreeSoA dendriticTreeSoA,
        DendriticCompetitionParamsSoA dendriticCompetitionParamsSoA
    ) {
        Objects.nonNull(synapseBranchSoA);
        Objects.nonNull(dendriticTreeSoA);
        Objects.nonNull(dendriticCompetitionParamsSoA);
        
        this.synapseBranchSoA = synapseBranchSoA;
        this.dendriticTreeSoA = dendriticTreeSoA;
        this.dendriticCompetitionParamsSoA = dendriticCompetitionParamsSoA;
    }

    public void applyCompetition( long now, int neuronId ) {
        int first = dendriticTreeSoA.firstBranchIndex[neuronId];
        int count = dendriticTreeSoA.branchCount[neuronId];

        if (count <= 1)
            return;

        // Precompute max activity (used by WTM and Normalized)
        float maxAct = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int bId = first + i;
            float act = synapseBranchSoA.branchActivity[bId];
            if (act > maxAct) maxAct = act;
        }

        // Apply competition per branch
        for (int i = 0; i < count; i++) {

            int bId = first + i;
            int type = synapseBranchSoA.getBranchTypeCode(bId);
            float act = synapseBranchSoA.branchActivity[bId];

            float inhib = 0f;

            switch (type) {
                case BranchTypeCode.NEAR:
                    // WinnerTakeMost
                    float delta = maxAct - act;
                    if (delta > 0f) {
                        float alpha = (float)(now - synapseBranchSoA.lastProcessTime[bId])
                                      / dendriticCompetitionParamsSoA.wtm_tauNanos[type];
                        if (alpha > 1f) alpha = 1f;
                        inhib = dendriticCompetitionParamsSoA.wtm_inhibitionLevel[type] * delta * alpha;
                    }
                    break;

                case BranchTypeCode.FAR:
                    // Continuous
                    float alphaC = (float)(now - synapseBranchSoA.lastProcessTime[bId])
                                   / dendriticCompetitionParamsSoA.cont_tauNanos[type];
                    if (alphaC > 1f) alphaC = 1f;
                    inhib = alphaC * dendriticCompetitionParamsSoA.cont_inhibitionMax[type] * act;
                    break;

                case BranchTypeCode.FEEDFORWARD:
                    // Normalized
                    float norm = act / (maxAct + dendriticCompetitionParamsSoA.norm_stability[type]);
                    inhib = dendriticCompetitionParamsSoA.norm_strength[type] * norm;
                    break;

                case BranchTypeCode.FEEDBACK:
                    // You can choose a model; here we reuse Continuous
                    float alphaF = (float)(now - synapseBranchSoA.lastProcessTime[bId])
                                   / dendriticCompetitionParamsSoA.cont_tauNanos[type];
                    if (alphaF > 1f) alphaF = 1f;
                    inhib = alphaF * dendriticCompetitionParamsSoA.cont_inhibitionMax[type] * act;
                    break;

                case BranchTypeCode.EXTERNAL:
                    continue;
            }

            synapseBranchSoA.inhibition[bId] += inhib;
        }
    }
}
