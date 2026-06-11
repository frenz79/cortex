package com.cortex.brain;

import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Commons;
import com.cortex.base.layers.AbstractLayer;
import com.cortex.base.plasticity.SynapsePlasticityConfig;

public class LayerConnectionsConfig {
	public int MIN_CONNECTIONS;
	public int MAX_CONNECTIONS;
	public float MAX_DISTANCE;
	public SynapsePlasticityConfig SYNAPSE_PLASTICITY_CONFIG;
	public Predicate<AbstractNeuron> NEURON_FILTER_PREDICATE = Commons.ALWAYS_CONNECT_PREDICATE;
	public AbstractLayer SOURCE_LAYER;
	public AbstractLayer TARGET_LAYER;
	public long BASE_SPEED;
	
	public static Builder newBuilder() {
        return new Builder();
    }   
    
    public static class Builder {
        private final LayerConnectionsConfig cfg;
        
        public Builder() {
            this.cfg = new LayerConnectionsConfig();
        }
        
        public Builder from(AbstractLayer SOURCE_LAYER) {
            cfg.SOURCE_LAYER = SOURCE_LAYER;
            return this;
        }
        
        public Builder withBaseSped(long  BASE_SPEED) {
            cfg.BASE_SPEED = BASE_SPEED;
            return this;
        }
        
        public Builder to(AbstractLayer TARGET_LAYER) {
            cfg.TARGET_LAYER = TARGET_LAYER;
            return this;
        }
        
        public Builder withConnections(int MIN_CONNECTIONS, int MAX_CONNECTIONS, float MAX_DISTANCE) {
            cfg.MIN_CONNECTIONS = MIN_CONNECTIONS;
            cfg.MAX_CONNECTIONS = MAX_CONNECTIONS;
            cfg.MAX_DISTANCE = MAX_DISTANCE;
            return this;
        }   
        
        public Builder withSynapsePlasticityConfig(SynapsePlasticityConfig SYNAPSE_PLASTICITY_CONFIG) {
            cfg.SYNAPSE_PLASTICITY_CONFIG = SYNAPSE_PLASTICITY_CONFIG;
            return this;
        }
        
        public Builder withNeuronFilter(Predicate<AbstractNeuron>  NEURON_FILTER_PREDICATE) {
            cfg.NEURON_FILTER_PREDICATE = NEURON_FILTER_PREDICATE;
            return this;
        }
        
        private void validate() {
        	 if (cfg.SOURCE_LAYER == null)
                 throw new IllegalArgumentException("SOURCE_LAYER must not be null");

             if (cfg.TARGET_LAYER == null)
                 throw new IllegalArgumentException("TARGET_LAYER must not be null");

             if (cfg.MIN_CONNECTIONS < 0)
                 throw new IllegalArgumentException("MIN_CONNECTIONS must be >= 0");

             if (cfg.MAX_CONNECTIONS < cfg.MIN_CONNECTIONS)
                 throw new IllegalArgumentException("MAX_CONNECTIONS must be >= MIN_CONNECTIONS");

             if (cfg.MAX_DISTANCE <= 0)
                 throw new IllegalArgumentException("MAX_DISTANCE must be > 0");

             if (cfg.SYNAPSE_PLASTICITY_CONFIG == null)
                 throw new IllegalArgumentException("SYNAPSE_PLASTICITY_CONFIG must not be null");

             if (cfg.NEURON_FILTER_PREDICATE == null)
                 throw new IllegalArgumentException("NEURON_FILTER_PREDICATE must not be null");          
        }
        
        public LayerConnectionsConfig build() {
            validate();
            return cfg;
        }
    }
}
