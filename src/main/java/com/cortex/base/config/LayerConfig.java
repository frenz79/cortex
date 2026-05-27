package com.cortex.base.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        	// Basic Validation
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
            return cfg.validate();
        }
    }
    
    public int getLayerId() {
        return LAYER_ID;
    }
    
    public LayerConfig validate() {

        List<String> errors = new ArrayList<>();

        // -------------------------
        // 1. VALIDAZIONE NEURONI
        // -------------------------
        CorticalNeuronsConfig n = this.CORTICAL_NEURONS_CONFIG;

        if (n.FIRING_THRESHOLD <= 0f || n.FIRING_THRESHOLD > 1.0f)
            errors.add("FIRING_THRESHOLD fuori range (0 < thr <= 1): " + n.FIRING_THRESHOLD);

        if (n.REPOLARIZATION_PER_NANOS < 0.01f || n.REPOLARIZATION_PER_NANOS > 0.50f)
            errors.add("REPOLARIZATION_PER_SECOND fuori range (0.01–0.50): " + n.REPOLARIZATION_PER_NANOS);

        if (n.RATE_DECAY_PER_WINDOW <= 0f || n.RATE_DECAY_PER_WINDOW >= 1f)
            errors.add("RATE_DECAY_PER_WINDOW deve essere (0 < x < 1): " + n.RATE_DECAY_PER_WINDOW);

        if (n.RATE_WINDOW_NANOS <= 0)
            errors.add("RATE_WINDOW deve essere > 0");

        // -------------------------
        // 2. VALIDAZIONE SINAPSI
        // -------------------------
        SynapsePlasticityConfig sp = this.SYNAPSE_PLASTICITY_CONFIG;

        ExcitatorySynapticPlasticityConfig e = sp.excitatory();
        InhibitorySynapticPlasticityConfig i = sp.inhibitory();

        // --- Eccitatoria ---
        if (e.INITIAL_WEIGHT < e.W_MIN || e.INITIAL_WEIGHT > e.W_MAX)
            errors.add("W_INITIAL non compreso tra W_MIN e W_MAX");

        if (e.W_BASELINE < e.W_MIN || e.W_BASELINE > e.W_MAX)
            errors.add("W_BASELINE non compreso tra W_MIN e W_MAX");

        if (e.A_PLUS <= 0f || e.A_MINUS <= 0f)
            errors.add("A_PLUS e A_MINUS devono essere > 0");

        if (e.A_PLUS <= e.A_MINUS)
            errors.add("A_PLUS deve essere > A_MINUS per avere potenziamento netto");

        if (e.TAU_PLUS <= 0 || e.TAU_MINUS <= 0)
            errors.add("TAU_PLUS e TAU_MINUS devono essere > 0");

    //    if (e.ELIGIBILITY_DECAY_NANOS*TimeUnit.s.toSeconds(1) < 0.95f 
     //   	|| e.ELIGIBILITY_DECAY_NANOS*TimeUnit.NANOSECONDS.toSeconds(1) > 0.999f)
      //      errors.add("ELIGIBILITY_DECAY fuori range consigliato (0.95–0.999)");

        if (e.HOMEOSTATIC_RATE < 0f || e.HOMEOSTATIC_RATE > 0.05f)
            errors.add("HOMEOSTATIC_RATE fuori range (0–0.05)");

        // --- Inibitoria ---
        if (i.INITIAL_WEIGHT < i.W_MIN || i.INITIAL_WEIGHT > i.W_MAX)
            errors.add("Inhibitory W_INITIAL non compreso tra W_MIN e W_MAX");

        if (i.LEARNING_RATE <= 0f || i.LEARNING_RATE > 0.02f)
            errors.add("Inhibitory LEARNING_RATE fuori range (0–0.02)");

        if (i.TARGET_FIRING_RATE <= 0f || i.TARGET_FIRING_RATE > 20f)
            errors.add("TARGET_FIRING_RATE fuori range (0–20)");

        // -------------------------
        // 3. VALIDAZIONE CONNESSIONI
        // -------------------------
        if (this.MIN_CONNECTIONS < 0)
            errors.add("minConnections deve essere >= 0");

        if (this.MAX_CONNECTIONS < this.MIN_CONNECTIONS)
            errors.add("maxConnections < minConnections");

        if (this.MAX_CONN_DISTANCE <= 0f || this.MAX_CONN_DISTANCE > 1.5f)
            errors.add("maxConnDistance fuori range (0–1.5)");

        // -------------------------
        // 4. VALIDAZIONE GEOMETRIA
        // -------------------------
        if (this.DIMENSION <= 0f || this.DIMENSION > 1.0f)
            errors.add("dimension deve essere (0–1]");

        if (this.INHIBITOR_FREQ < 0f || this.INHIBITOR_FREQ > 1f)
            errors.add("inhibitorFreq deve essere (0–1)");

        // -------------------------
        // 5. VALIDAZIONE CROSS-LAYER (se vuoi)
        // -------------------------
        // Esempio: layer più profondi devono essere più selettivi
        if (this.LAYER_ID > 0) {
            if (n.FIRING_THRESHOLD < 0.05f)
                errors.add("Layer " + LAYER_ID + ": FIRING_THRESHOLD troppo basso per un layer profondo");

            if (n.REPOLARIZATION_PER_NANOS < 0.05f)
                errors.add("Layer " + LAYER_ID + ": REPOLARIZATION troppo bassa (rischio runaway)");
        }

        // -------------------------
        // 6. RISULTATO
        // -------------------------
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                "LayerConfig " + LAYER_ID + " non valido:\n" +
                errors.stream().map(s -> " - " + s).collect(Collectors.joining("\n"))
            );
        }
        return this;
    }
}