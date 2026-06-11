package com.cortex.base.lateralinhibition;

import com.cortex.base.AbstractNeuron.SynapseBranch;

public interface ILateralInhibitionStrategy {
	 void updateInhibition(SynapseBranch[] branches, long deltaTimeNanos);
}
