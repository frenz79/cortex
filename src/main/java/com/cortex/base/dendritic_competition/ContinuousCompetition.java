package com.cortex.base.dendritic_competition;

import com.cortex.base.SynapseBranch;
import com.cortex.base.utils.Maths;

/**
 * Smooth competition, cooperative model
 * 
 * - more active branches -> less inhibited
 * - less active branches -> more inhibited
 */
public class ContinuousCompetition implements IDendriticCompetitionStrategy {

	private final ContinuousCompetitionConfig cfg;
	
	public ContinuousCompetition(ContinuousCompetitionConfig cfg) {
		super();
		this.cfg = cfg;
	}
	
	@Override
	public void updateCompetition(SynapseBranch[] branches, long dt) {
	    float maxActivity = 0f;
	    for (SynapseBranch b : branches)
	        if (b.branchActivity > maxActivity)
	            maxActivity = b.branchActivity;

	    // dt/tau
	    float alpha = (float) dt / (float) cfg.INHIBITION_TAU_NANOS;
	    if (alpha > 1f) alpha = 1f;
	    if (alpha < 0f) alpha = 0f;

	    for (SynapseBranch b : branches) {
	        // differenza rispetto al branch più attivo
	        float target = maxActivity - b.branchActivity;
	        // update continuo
	        b.inhibition += alpha * target;
	        // clamp
	        b.inhibition = Maths.clamp(b.inhibition, 0f, cfg.INHIBITION_MAX);
	    }
	}
}