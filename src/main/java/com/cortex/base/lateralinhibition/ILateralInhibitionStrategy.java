package com.cortex.base.lateralinhibition;

import com.cortex.base.SynapseBranch;

public interface ILateralInhibitionStrategy {
	 void updateInhibition(SynapseBranch[] branches, long deltaTimeNanos);
}
