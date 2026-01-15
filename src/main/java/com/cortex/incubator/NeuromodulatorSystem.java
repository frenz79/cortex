package com.cortex.incubator;

/**
 
 NEUROMODULATORI (dopamina-like)
Concetto chiave (importante)

I neuromodulatori:

NON trasmettono informazione

NON sono spike-driven classici

MODULANO la plasticità, non il firing

👉 In pratica: scalano il learning rate globale o locale

🎯 Architettura consigliata

Un NeuromodulatorSystem globale

Uno o più NeuromodulatorNeuron

Un segnale continuo [-1 … +1]

Nessuna sinapsi classica
  
 */
public class NeuromodulatorSystem {

	private volatile float dopamine = 0f;
	private final float DECAY = 0.98f;
	private final float MAX = 1.0f;

	public void reward(float amount) {
		dopamine += amount;
		dopamine = clamp(dopamine, -MAX, MAX);
	}

	public float getSignal() {
		return dopamine;
	}

	public void update() {
		dopamine *= DECAY;
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
