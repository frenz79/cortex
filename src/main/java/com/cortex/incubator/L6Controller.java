package com.cortex.incubator;

/**
 L6 – Layer di controllo / attenzione
Ruolo

monitora:

errore

reward

incertezza

modula:

soglie L4

plasticità

sampling retina
  
  
 */
public class L6Controller {
	private float gain = 1.0f;

	public void update(float error, float reward) {
		gain += 0.1f * error - 0.05f * reward;
		//    gain = clamp(gain, 0.5f, 2.0f);
	}

	public float getGain() {
		return gain;
	}

	/*
	 L4Neuron.threshold *= L6.getGain();
	 Retina.setSensitivity(L6.getGain());
	 Plasticity.setLearningRate(L6.getGain());
 
	 */

}
