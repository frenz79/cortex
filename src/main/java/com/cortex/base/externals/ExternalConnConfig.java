package com.cortex.base.externals;

import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.plasticity.SynapsePlasticityConfig;

public class ExternalConnConfig {
	
	public int CONNECTIONS;
	public float MAX_DISTANCE;
	public SynapsePlasticityConfig SYNAPSE_PLASTICITY_CONFIG;
	public Predicate<AbstractNeuron> NEURON_FILTER_PREDICATE;
	public int LIKED_LAYER_ID;
	
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final ExternalConnConfig cfg;

        public Builder() {
            cfg = new ExternalConnConfig();
            // default sensati
            cfg.CONNECTIONS = -1;
            cfg.MAX_DISTANCE = Float.MAX_VALUE;
            cfg.NEURON_FILTER_PREDICATE = n -> true;
            cfg.LIKED_LAYER_ID = -1;
        }

        public Builder withConnections(int connections) {
            cfg.CONNECTIONS = connections;
            return this;
        }

        public Builder withLikedLayerId(int layerId) {
            cfg.LIKED_LAYER_ID = layerId;
            return this;
        }
        
        public Builder withMaxDistance(float maxDist) {
            cfg.MAX_DISTANCE = maxDist;
            return this;
        }

        public Builder withPlasticity(SynapsePlasticityConfig spc) {
            cfg.SYNAPSE_PLASTICITY_CONFIG = spc;
            return this;
        }

        public Builder withNeuronFilter(Predicate<AbstractNeuron> filter) {
            cfg.NEURON_FILTER_PREDICATE = filter;
            return this;
        }

        private void validate() {
            if (cfg.CONNECTIONS < 0)
                throw new IllegalArgumentException("MIN_CONNECTIONS < 0");

            if (cfg.MAX_DISTANCE <= 0)
                throw new IllegalArgumentException("MAX_DISTANCE must be > 0");

            if (cfg.NEURON_FILTER_PREDICATE == null)
                throw new IllegalArgumentException("NEURON_FILTER_PREDICATE cannot be null");
            
            if (cfg.LIKED_LAYER_ID <0)
                throw new IllegalArgumentException("LIKED_LAYER_ID not set or invalid");
        }

        public ExternalConnConfig build() {
            validate();
            return cfg;
        }
    }
}
