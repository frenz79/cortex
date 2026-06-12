package com.cortex.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.brain.Brain;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventListener;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public class MetricsRecorder {

	protected final Logger logger = LogManager.getLogger(this.getClass());
	
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
				try {
					Synapse s =(Synapse)source;
					int layerId = s.getSource().getLayerId();
					if (layerId<0) return;
					
			        layersStats.computeIfAbsent(layerId, 
			           	k -> new MetricsLayerRecorder(brain.getLayer(layerId)))
			        		.updateSynapticStatistics( (Synapse)source, (SynapseUpdatedData)data);
				} catch (Exception ex) {
					logger.error("Handled Exception:", ex);
				}
			}
		});
	}
	
    private LayerStats getAndResetStats(int layerId) {
        MetricsLayerRecorder s = layersStats.get(layerId);
        return (s != null) ? s.getStatsAndReset() : null;
    }
    
    public void pollLayerStats(long time, long avgProcTime, long runs, int layerId) {
    	//logger.info("Calculating layers stats..");
    	LayerStats stats = getAndResetStats(layerId);

    	if (dumpStatsTimeNanos>0 && time - lastDumpTimeNanos > dumpStatsTimeNanos) {
			System.out.println("== Now: "+time+" - Avg Time:" + String.format("%,.2f",(avgProcTime/1000.0f)) + "ms Runs:"+runs+" ===========");
    		dumpStats(time);
    		lastDumpTimeNanos = time;
    	}
    	
    	EventBus.fire(EventType.LAYER_STATS, time, this, stats);
    }
    
	private void dumpStats(long time) {
		try {
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
					+ " | PLAST " 
					+ " | ENERGY" 
					);
			
			for (Abstract3DLayer l : brain.getAllLayers()) {
				LayerStats stats = getAndResetStats(l.getLayerId());
				if (stats != null) {
					System.out.println(
							l.getLayerId() 
							+ " | " + String.format("%,7d",stats.totalNeurons() )
							+ " | " + String.format("%,3d",stats.activeNeurons() )
							+ " | " + String.format("%,8.2f",stats.avgFiringRateActive() )
							+ " | " + String.format("%,8.2f",stats.avgFiringRateAll() )
							+ " | " + String.format("%,5.2f",stats.averageSynapticWeight() )
							+ " | " + String.format("%,9.2f",stats.synapticWeightStdDev() )							
							+ " | " + String.format("%,7.2f",stats.saturatedMaxRatio() )
							+ " | " + String.format("%,7.2f",stats.saturatedMinRatio() )
							+ " | " + String.format("%,6.4f",stats.sparsity() )
							+ " | " + String.format("%,6.2f",stats.totalPlasticity())
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