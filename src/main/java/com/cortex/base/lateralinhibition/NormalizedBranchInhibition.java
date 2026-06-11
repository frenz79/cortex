package com.cortex.base.lateralinhibition;

import com.cortex.base.SynapseBranch;

/**
 * Dendritic Softmax
 * 
 * - all branches contribute -> none is inhibited
 * - high stability
 */
public class NormalizedBranchInhibition implements ILateralInhibitionStrategy {

	private final NormalizedBranchInhibitionConfig cfg;
	
	public NormalizedBranchInhibition(NormalizedBranchInhibitionConfig cfg) {
		super();
		this.cfg = cfg;
	}
	
	@Override
	public void updateInhibition(SynapseBranch[] branches, long dt) {

	    float sum = 0f;
	    for (SynapseBranch b : branches)
	        sum += Math.max(0f, b.branchActivity);

	    // stabilità numerica
	    if (sum < 1e-4f) sum = 1e-4f;

	    // calcola media (utile per stabilizzare)
	    float mean = sum / branches.length;

	    for (SynapseBranch b : branches) {

	        // normalizzazione stile softmax lineare
	        float norm = b.branchActivity / sum;

	        // deviazione dalla media
	        float deviation = (b.branchActivity - mean);

	        // inibizione = softmax + stabilizzazione
	        b.inhibition =
	                cfg.NORMALIZATION_STRENGTH * (1f - norm)
	                + cfg.STABILITY_FACTOR * (-deviation);

	        // clamp
	        if (b.inhibition < 0f) b.inhibition = 0f;
	    }
	}
}