package com.cortex.metrics;

import java.util.concurrent.ConcurrentHashMap;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.brain.Brain;
import com.cortex.brain.layers.Layer;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventListener;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseUpdatedData;
import com.cortex.globals.GlobalContext;

public class MetricsRecorder {

	private final Brain brain;
	private final ConcurrentHashMap<Integer, MetricsLayerRecorder> layersStats = new ConcurrentHashMap<>();	
	
	public MetricsRecorder( Brain brain ) {
		this.brain = brain;
		
		EventBus.addListener(EventType.NEURON_FIRED,  new EventListener() {
			
			@Override
			public void onEvent(EventType type, long time, Object source, Object data) {
				AbstractNeuron n =(AbstractNeuron)source;
				int layerId = n.getLayerId();
				
				if (layerId<0) return;
		        layersStats.computeIfAbsent(layerId, 
		        	k -> new MetricsLayerRecorder(brain.getLayer(layerId)))
		        		.neuronFired(n);
			}
		});
		
		EventBus.addListener(EventType.SYNAPSE_UPDATED,  new EventListener() {
			
			@Override
			public void onEvent(EventType type, long time, Object source, Object data) {
				Synapse s =(Synapse)source;
				int layerId = s.getSource().getLayerId();
				if (layerId<0) return;
				
		        layersStats.computeIfAbsent(layerId, 
		           	k -> new MetricsLayerRecorder(brain.getLayer(layerId)))
		        		.sumSynapticWeights((SynapseUpdatedData)data);
			}
		});
	}
	
    private LayerStats getAndResetStats(int layerId) {
        MetricsLayerRecorder s = layersStats.get(layerId);
        return (s != null) ? s.getStatsAndReset() : null;
    }
    
    public LayerStats pollLayerStats(int layerId) {
        return getAndResetStats(layerId);
    }
    
	public void dumpStats() {
		try {
			System.out.println("== Avg Process Time:" + GlobalContext.getAverageProcessTimeMillis() + "ms ===========");
			System.out.println(
					  "L" 
					+ " | NEURONS" 
					+ " | ACT"
					+ " | SYN_W"
					+ " | SYN_W_STD" 
					+ " | FIRE_ACT" 
					+ " | FIRE_ALL" 
					+ " | SAT_MAX" 
					+ " | SAT_MIN"
					+ " | SPARSE" 
					+ " | PLAST" 
					+ " | ENERGY" 
					);
			
			for (Layer l : brain.getAllLayers()) {
				LayerStats stats = getAndResetStats(l.getLayerId());
				if (stats != null) {
					System.out.println(
							l.getLayerId() 
							+ " | " + stats.totalNeurons() 
							+ " | " + stats.activeNeurons()
							+ " | " + String.format("%,.2f",stats.averageSynapticWeight() )
							+ " | " + String.format("%,.2f",stats.synapticWeightStdDev() )
							+ " | " + String.format("%,.2f",stats.avgFiringRateActive() )
							+ " | " + String.format("%,.2f",stats.avgFiringRateAll() )
							+ " | " + String.format("%,.2f",stats.saturatedMaxRatio() )
							+ " | " + String.format("%,.2f",stats.saturatedMinRatio() )
							+ " | " + String.format("%,.2f",stats.sparsity() )
							+ " | " + String.format("%,.2f",stats.totalPlasticity())
							+ " | " + String.format("%,.2f",stats.energy() )
							);
					
				} else {
					System.out.println("Layer:" + l.getLayerId() + " NO STATS");
				}
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}
}