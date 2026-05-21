package com.cortex.layer;

import com.cortex.base.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.InhibitorySynapticPlasticityConfig;

public record SynapsePlasticityConfig(
	ExcitatorySynapticPlasticityConfig excitatorySynapticPlasticityConfig,
	InhibitorySynapticPlasticityConfig inhibitorySynapticPlasticityConfig
) {
	
}
