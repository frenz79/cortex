package com.cortex.base.soa.logic;

import com.cortex.base.soa.DendriticCompetitionParamsSoA;
import com.cortex.base.soa.DendriticTreeSoA;
import com.cortex.base.soa.SynapseBranchSoA;

public final class DendriticCompetitionLogicSoA {

    private final SynapseBranchSoA branchState;
    private final DendriticTreeSoA dendriticTree;
    private final DendriticCompetitionParamsSoA params;

    public DendriticCompetitionLogicSoA(
        SynapseBranchSoA branchState,
        DendriticTreeSoA dendriticTree,
        DendriticCompetitionParamsSoA params
    ) {
        this.branchState = branchState;
        this.dendriticTree = dendriticTree;
        this.params = params;
    }

    public void applyCompetition(int neuronId, long now) {

        int first = dendriticTree.firstBranchIndex[neuronId];
        int count = dendriticTree.branchCount[neuronId];

        if (count <= 1)
            return;

        // Precompute max activity (used by WTM and Normalized)
        float maxAct = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int bId = first + i;
            float act = branchState.branchActivity[bId];
            if (act > maxAct) maxAct = act;
        }

        // Apply competition per branch
        for (int i = 0; i < count; i++) {

            int bId = first + i;
            int type = branchState.getBranchTypeCode(bId);
            float act = branchState.branchActivity[bId];

            float inhib = 0f;

            switch (type) {
                case SynapseBranchSoA.BranchTypeCode.NEAR:
                    // WinnerTakeMost
                    float delta = maxAct - act;
                    if (delta > 0f) {
                        float alpha = (float)(now - branchState.lastProcessTime[bId])
                                      / params.wtm_tauNanos[type];
                        if (alpha > 1f) alpha = 1f;
                        inhib = params.wtm_inhibitionLevel[type] * delta * alpha;
                    }
                    break;

                case SynapseBranchSoA.BranchTypeCode.FAR:
                    // Continuous
                    float alphaC = (float)(now - branchState.lastProcessTime[bId])
                                   / params.cont_tauNanos[type];
                    if (alphaC > 1f) alphaC = 1f;
                    inhib = alphaC * params.cont_inhibitionMax[type] * act;
                    break;

                case SynapseBranchSoA.BranchTypeCode.FEEDFORWARD:
                    // Normalized
                    float norm = act / (maxAct + params.norm_stability[type]);
                    inhib = params.norm_strength[type] * norm;
                    break;

                case SynapseBranchSoA.BranchTypeCode.FEEDBACK:
                    // You can choose a model; here we reuse Continuous
                    float alphaF = (float)(now - branchState.lastProcessTime[bId])
                                   / params.cont_tauNanos[type];
                    if (alphaF > 1f) alphaF = 1f;
                    inhib = alphaF * params.cont_inhibitionMax[type] * act;
                    break;

                case SynapseBranchSoA.BranchTypeCode.EXTERNAL:
                    continue;
            }

            branchState.inhibition[bId] += inhib;
        }
    }
}
