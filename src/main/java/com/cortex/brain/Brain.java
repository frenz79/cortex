package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.Synapse;
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
	
	public Brain build( ) {

		return this;
	}

	public static Builder newBuilder() {
		return new Builder();
	}   

	public static class Builder {
		private final Brain brain;

		public Builder() {
			this.brain = new Brain();
		}

		public Builder addEmisphere(Emisphere<?> em) {
			brain.emispheres.add(em);
			return this;
		} 
		
		public Builder addEmispheres(Emisphere<?>[] em) {
			for (Emisphere<?>e : em) {
				brain.emispheres.add(e);
			}
			return this;
		}
		
		public Builder attachSensor(ISensor s, Emisphere<?> em) {
			em.attachSensor(Objects.requireNonNull(s));
			return this;
		}

		public Builder attachActuator(IActuator a, Emisphere<?> em) {
			em.attachActuator(Objects.requireNonNull(a));
			return this;
		}

		public Builder attachClassifier(IClassifier<?> c, Emisphere<?> em) {
			em.attachClassifier(Objects.requireNonNull(c));
			return this;
		}

		public Builder attachSupervisor(ISupervisor<?> s, Emisphere<?> em) {
			em.attachSupervisor(Objects.requireNonNull(s));
			return this;
		}

		private void validate() {

		}

		public Brain build() {
			validate();
			return brain.build();
		}
	}

	public List<Emisphere<?>> getEmispheres() {
		return emispheres;
	}
	
	public Emisphere<?> getEmisphere(int id) {
		return emispheres.get(id);
	}

	public int getNeuronsCount() {
		int ret = 0;
		for ( Emisphere<?> e : emispheres ) {
			ret += e.getAllNeurons().length;
		}
		return ret;
	}

	public int getSynapsesCount() {
		return Synapse.getSynapsesCount();
	}
}
