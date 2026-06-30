package com.cortex.base.lateral_inhibition;

import com.cortex.base.AbstractNeuron;

public interface ILateralInhibitionStrategy {
	
	 void updateInhibition(long deltaTimeNanos, AbstractNeuron n, float gain);
	 
}
