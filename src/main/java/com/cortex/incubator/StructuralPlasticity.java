package com.cortex.incubator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;

/**
 PLASTICITÀ STRUTTURALE
 (pruning + growth)
  
  Concetto biologico (semplificato)
	sinapsi deboli e inutili → eliminate
	neuroni attivi ma isolati → cercano nuove connessioni
	crescita lenta, pruning aggressivo

  Politica semplice e robusta
	Pruning
		Una sinapsi viene rimossa se:
		peso < PRUNE_THRESHOLD
		inutilizzata per T_PRUNE
	Growth
		Un neurone può:
		creare N nuove sinapsi
		verso neuroni spazialmente vicini
		con bias verso eccitatori
		
  Effetti emergenti (veri, non teorici)
	Con 3 + 4 attivi, il tuo sistema:
	forma cluster funzionali
	elimina neuroni inutili
	specializza L4 per feature
	specializza L_out per task
	riusa la stessa rete per più attuatori

  
  Dove abilitarla (IMPORTANTISSIMO)
  Connessione	Structural plasticity
	Retina → L1	❌
	L1 ↔ L1	❌
	L2 ↔ L3	⚠️ (solo pruning)
	L3 → L4	✅
	L4 → L_out	✅
	Feedback	❌
  
  Caller Ogni X tick (es. 1000)
  
  for (Synapse s : neuron.getOutSynapses()) {
    if (StructuralPlasticity.shouldPrune(s, now)) {
        neuron.removeSynapse(s);
    }
}

if (neuron.getActivityLevel() > HIGH_ACTIVITY) {
    List<Synapse> newOnes =
        StructuralPlasticity.growSynapses(neuron, nearbyNeurons, now);
    neuron.addSynapses(newOnes);
}
  
 */
public class StructuralPlasticity {
	private static final float PRUNE_THRESHOLD = 0.05f;
    private static final long PRUNE_TIME = 5_000; // tick
    private static final int MAX_NEW_SYNAPSES = 2;
/*
    public static boolean shouldPrune(Synapse s, long now) {
        return s.getWeight() < PRUNE_THRESHOLD &&
               (now - s.getLastUsedTime()) > PRUNE_TIME;
    }

    public static List<Synapse> growSynapses(
            AbstractNeuron source,
            List<AbstractNeuron> candidates,
            long now) {

        List<Synapse> created = new ArrayList<>();

        candidates.stream()
            .filter(n -> !source.isConnectedTo(n))
            .sorted(Comparator.comparingDouble(n -> distance(source, n)))
            .limit(MAX_NEW_SYNAPSES)
            .forEach(target -> {
                Synapse s = new Synapse(source, target);
                created.add(s);
            });

        return created;
    }

    private static double distance(AbstractNeuron a, AbstractNeuron b) {
        return a.getPosition().dist(b.getPosition());
    }
*/
}
