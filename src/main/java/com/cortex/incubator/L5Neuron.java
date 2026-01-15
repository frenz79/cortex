package com.cortex.incubator;

import com.cortex.base.AbstractNeuron;

/*
 
  Layer 5 – OUTPUT MOTORIO / DECISIONALE

neuroni piramidali giganti
proiettano:
	a strutture subcorticali
	a midollo / motoneuroni
	ad altri sistemi motori
segnali:
	rari
	forti
	decisionali
	
	
	L5 – Layer di decisione / attuazione
Ruolo

riceve input da L4

produce spike solo quando c’è consenso

collega direttamente agli attuatori

Proprietà

soglia alta

refrattario lungo

plasticità limitata
  
 */
public class L5Neuron  {
	 private static final float THRESHOLD = 300;
	    private static final long REFRACTORY = 50;

	   // @Override
	    public void process(long now) {
	   //     integrateInputs(now);
	   //    if (potential > THRESHOLD && now - lastSpikeTime > REFRACTORY) {
	   //         fire(now);
	   //         potential = 0;
	   //     }
	    }
}
