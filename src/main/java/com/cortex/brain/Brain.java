package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.layers.AbstractLayer;
import com.cortex.base.layers.Emisphere;
import com.cortex.base.layers.Emisphere.CorticalNeuronFactory;
import com.cortex.base.layers.LayerConfig;
import com.cortex.base.layers.SphericalLayer;
import com.cortex.base.modules.IActuator;
import com.cortex.base.modules.IClassifier;
import com.cortex.base.modules.ISensor;
import com.cortex.base.modules.ISupervisor;

public class Brain {

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	private List<Emisphere<?>> emispheres = new ArrayList<>();
	
	public void process(long now) {
		for ( var e : emispheres) {
			e.process(now);
		}
	}	

	public static Builder newBuilder() {
		return new Builder();
	}   

	public static class Builder {
		private final Brain brain;

		public Builder() {
			this.brain = new Brain();
		}

		public Builder addLayerConfig(LayerConfig cfg) {
			brain.layersConfigs.add(cfg);
			return this;
		}

		public Builder addLayerConnectionConfig(BrainLayersConnConfig cfg) {
			brain.brainLayersConnConfig = cfg;
			return this;
		}

		public Builder withTotalNeurons(int totalNeurons) {
			brain.totalNeurons = totalNeurons;
			return this;
		}	     

		private void validate() {

		}

		public Brain build() {
			validate();
			return brain.build();
		}
	}
}
