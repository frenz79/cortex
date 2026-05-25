package com.cortex.base.config;

public record SynapsePlasticityConfig(
	ExcitatorySynapticPlasticityConfig excitatory,
	InhibitorySynapticPlasticityConfig inhibitory
) {
	
}
