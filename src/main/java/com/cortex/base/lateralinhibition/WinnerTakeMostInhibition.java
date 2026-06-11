package com.cortex.base.lateralinhibition;

import com.cortex.base.AbstractNeuron.SynapseBranch;

/**
 * High competition, selective model
 * 
 * - only one branch wins -> others are inhibited
 * - meant for features extraction 
 */
public class WinnerTakeMostInhibition implements ILateralInhibitionStrategy {

	private final WinnerTakeMostInhibitionConfig cfg;

	public WinnerTakeMostInhibition(WinnerTakeMostInhibitionConfig cfg) {
		super();
		this.cfg = cfg;
	}

	@Override
	public void updateInhibition(SynapseBranch[] branches, long dt) {

		// trova il branch vincitore
		SynapseBranch winner = null;
		float max = -Float.MAX_VALUE;

		for (SynapseBranch b : branches) {
			if (b.branchActivity > max) {
				max = b.branchActivity;
				winner = b;
			}
		}

		// dt/tau
		float alpha = (float) dt / (float) cfg.WTA_TAU_NANOS;
		if (alpha > 1f) alpha = 1f;
		if (alpha < 0f) alpha = 0f;

		// aggiorna inibizione in modo continuo
		for (SynapseBranch b : branches) {

			float target = (b == winner) ? 0f : cfg.WTA_INHIBITION_LEVEL;

			// dinamica continua
			b.inhibition += alpha * (target - b.inhibition);

			// clamp
			if (b.inhibition < 0f) b.inhibition = 0f;
			if (b.inhibition > cfg.WTA_INHIBITION_LEVEL)
				b.inhibition = cfg.WTA_INHIBITION_LEVEL;
		}
	}
}