package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.externals.AbstractExternalModuleLogic;

public class Brain {

	private final Logger logger = LogManager.getLogger(this.getClass());
	
	private List<Hemisphere<?>> emispheres = new ArrayList<>();
	
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

		public Builder addEmisphere(Hemisphere<?> em) {
			brain.emispheres.add(em);
			return this;
		} 
		
		public Builder addEmispheres(Hemisphere<?>[] em) {
			for (Hemisphere<?>e : em) {
				brain.emispheres.add(e);
			}
			return this;
		}

		private void validate() {

		}

		public Brain build() {
			validate();
			return brain.build();
		}
	}

	public List<Hemisphere<?>> getEmispheres() {
		return emispheres;
	}
	
	public Hemisphere<?> getEmisphere(int id) {
		return emispheres.get(id);
	}

	public int getNeuronsCount() {
		int ret = 0;
		for ( Hemisphere<?> e : emispheres ) {
			ret += e.getTotalNeurons();		
		}
		return ret;
	}

	public int getSynapsesCount() {
		int ret = 0;
		for ( Hemisphere<?> e : emispheres ) {
			ret += e.getTotalSynapses();		
		}
		return ret;
	}

	public void attachExternalModule(AbstractExternalModuleLogic extLogic) {
		getEmisphere(extLogic.getTargetHemisphereId())
			.attachExternalModule(extLogic);
	}
}
