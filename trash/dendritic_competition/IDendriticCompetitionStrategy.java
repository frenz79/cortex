package com.cortex.base.dendritic_competition;

import com.cortex.base.SynapseBranch;

public interface IDendriticCompetitionStrategy {
	 void updateCompetition(SynapseBranch[] branches, long deltaTimeNanos);
}
