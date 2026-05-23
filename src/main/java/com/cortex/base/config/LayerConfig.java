package com.cortex.base.config;

import java.util.function.Predicate;

import com.cortex.base.AbstractNeuron;

public class LayerConfig {
        
    private final int LAYER_ID;

    // CONFIG PARAMETERS (UPPERCASE + PUBLIC)
    public int NEURONS_COUNT = 0;
    public float INHIBITOR_FREQ = 0;
    public int MIN_CONNECTIONS = 0;
    public int MAX_CONNECTIONS = 0;
    public boolean HAS_INCOMING = false;
    public boolean HAS_OUTGOING = false;
    public float MAX_CONN_DISTANCE = 0;
    public Predicate<AbstractNeuron> CONNECTION_FILTER = null;
    public SynapsePlasticityConfig SYNAPSE_PLASTICITY_CONFIG = null;
    public CorticalNeuronsConfig CORTICAL_NEURONS_CONFIG = null;
    public float DIMENSION = 0;
    
    LayerConfig(int layerId) {
        this.LAYER_ID = layerId;
    }

    public static Builder newBuilder(int layerId) {
        return new Builder(layerId);
    }   
    
    public static class Builder {
        private final LayerConfig cfg;
        
        public Builder(int layerId) {
            this.cfg = new LayerConfig(layerId);
        }
        
        public Builder enableInConn(boolean hasIncoming) {
            cfg.HAS_INCOMING = hasIncoming;
            return this;
        }
        
        public Builder enableOutConn(boolean hasOutgoing) {
            cfg.HAS_OUTGOING = hasOutgoing;
            return this;
        }
        
        public Builder withDimension(float dimension) {
            cfg.DIMENSION = dimension;
            return this;
        }
        
        public Builder withNeurons(int neuronsCount) {
            cfg.NEURONS_COUNT = neuronsCount;
            return this;
        }
        
        public Builder withInhibitorFreq(float inhibitorFreq) {
            cfg.INHIBITOR_FREQ = inhibitorFreq;
            return this;
        }
        
        public Builder withConnection(int minConnections, int maxConnections, float maxConnDistance) {      
            cfg.MIN_CONNECTIONS = minConnections;
            cfg.MAX_CONNECTIONS = maxConnections;
            cfg.MAX_CONN_DISTANCE = maxConnDistance;
            return this;
        }
        
        public Builder withConnectionFilter(Predicate<AbstractNeuron> connectionFilter) {
            cfg.CONNECTION_FILTER = connectionFilter;
            return this;
        }
        
        public Builder withSynapsePlasticityConfig(SynapsePlasticityConfig synapsePlasticityConfig) {
            cfg.SYNAPSE_PLASTICITY_CONFIG = synapsePlasticityConfig;
            return this;
        }
        
        public Builder withCorticalNeuronsConfig(CorticalNeuronsConfig CORTICAL_NEURONS_CONFIG) {
            cfg.CORTICAL_NEURONS_CONFIG = CORTICAL_NEURONS_CONFIG;
            return this;
        }
        
        private void validate() {

            if (cfg.MAX_CONNECTIONS < cfg.MIN_CONNECTIONS)
                throw new IllegalArgumentException("MAX_CONNECTIONS must be >= MIN_CONNECTIONS");

            if (cfg.MIN_CONNECTIONS < 0 || cfg.MAX_CONNECTIONS <= 0)
                throw new IllegalArgumentException("Invalid MIN/MAX_CONNECTIONS parameters");

            if (cfg.NEURONS_COUNT <= 0)
                throw new IllegalArgumentException("Invalid NEURONS_COUNT parameter");

            if (cfg.MAX_CONN_DISTANCE <= 0 || cfg.MAX_CONN_DISTANCE > cfg.DIMENSION)
                throw new IllegalArgumentException("Invalid MAX_CONN_DISTANCE / DIMENSION parameters");

            if (!cfg.HAS_INCOMING && !cfg.HAS_OUTGOING)
                throw new IllegalArgumentException("Layer must have incoming or outgoing connections");
            
            if (cfg.CORTICAL_NEURONS_CONFIG==null)
                throw new IllegalArgumentException("Layer must have CORTICAL_NEURONS_CONFIG");
        }
        
        public LayerConfig build() {
            validate();
            return cfg;
        }
    }
    
    public int getLayerId() {
        return LAYER_ID;
    }
}