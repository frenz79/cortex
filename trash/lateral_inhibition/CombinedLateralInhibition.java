package com.cortex.base.lateral_inhibition;

import com.cortex.base.AbstractNeuron;

public class CombinedLateralInhibition implements ILateralInhibitionStrategy {

    private final ILateralInhibitionStrategy topo;
    private final ILateralInhibitionStrategy syn;
    private final float topoWeight;
    private final float synWeight;

    public CombinedLateralInhibition(
        float topoStrength,
        float synStrength ,
        float topoWeight,
        float synWeight
    ) {
        this.topo = new TopographicLateralInhibition(topoStrength);
        this.syn = new SynapticalLateralInhibition(synStrength);
        this.topoWeight = topoWeight;
        this.synWeight = synWeight;
    }

    @Override
    public void updateInhibition(long dt, AbstractNeuron n, float gain/*not used here*/) {
        if (topoWeight > 0f)
            topo.updateInhibition(dt, n, topoWeight);

        if (synWeight > 0f)
            syn.updateInhibition(dt, n, synWeight);
    }
}
