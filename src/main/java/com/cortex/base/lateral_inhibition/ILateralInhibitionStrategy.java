package com.cortex.base.lateral_inhibition;

import com.cortex.base.SynapseBranch;

public interface ILateralInhibitionStrategy {
	 void updateInhibition(SynapseBranch[] branches, long deltaTimeNanos);
}
