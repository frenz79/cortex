package com.cortex.layer;

import com.cortex.base.ExcitatorySynapticPlasticity;
import com.cortex.base.InhibitorySynapticPlasticity;

public record SynapsePlasticityConfig(
	ExcitatorySynapticPlasticity excitatorySynapticPlasticity,
	InhibitorySynapticPlasticity inhibitorySynapticPlasticity
){	
}
