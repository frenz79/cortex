package com.cortex.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
	
	private final long dumpStatsTimeNanos;
	private long lastDumpTimeNanos = System.nanoTime();
	
	public MetricsRecorder( Brain brain, long dumpStatsTimeMillis ) {
		this.brain = brain;
		this.dumpStatsTimeNanos = TimeUnit.MILLISECONDS.toNanos(dumpStatsTimeMillis);
		
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
    
    public LayerStats pollLayerStats(long time, int layerId) {
    	LayerStats stats = getAndResetStats(layerId);
    	EventBus.fire(EventType.LAYER_STATS, time, this, stats);
    	
    	if (dumpStatsTimeNanos>0 && time - lastDumpTimeNanos > dumpStatsTimeNanos) {
    		dumpStats(time);
    		lastDumpTimeNanos = time;
    	}
        return stats;
    }
    
	private void dumpStats(long time) {
		try {
			System.out.println("== Time: "+time+" - Avg Process Time:" + GlobalContext.getAverageProcessTimeMillis() + "ms ===========");
			System.out.println(
					  "L" 
					+ " | NEURONS" 
					+ " | ACT"
					+ " | FIRE_ACT" 
					+ " | FIRE_ALL" 
					+ " | SYN_W"
					+ " | SYN_W_STD" 
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
							+ " | " + String.format("%,.2f",stats.avgFiringRateActive() )
							+ " | " + String.format("%,.2f",stats.averageSynapticWeight() )
							+ " | " + String.format("%,.2f",stats.synapticWeightStdDev() )
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