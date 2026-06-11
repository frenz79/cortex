package com.cortex.base.plasticity;

public record SynapsePlasticityConfig(
	ExcitatorySynapticPlasticityConfig excitatory,
	InhibitorySynapticPlasticityConfig inhibitory
) {
	
}
