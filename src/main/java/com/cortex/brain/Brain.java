package com.cortex.brain;

import java.util.function.Function;

import javax.vecmath.Point3f;

import com.cortex.base.config.CorticalNeuronsConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.brain.layers.MultiLayer;
import com.cortex.brain.layers.MultiLayerConfig;
import com.cortex.brain.layers.SphericalLayer;

public class Brain extends MultiLayer<SphericalLayer>{

	private volatile CorticalNeuron[] neurons;
	private final CorticalNeuronFactory neuronFactory;
	
	public class CorticalNeuronFactory {
		
		private final MultiLayerConfig config;
		private int counter = 0;
		
		public synchronized CorticalNeuron buildNeuron( 
			int layerId, 
			boolean inhibitor, 
			Point3f position
		) {
			CorticalNeuron n = new CorticalNeuron(
				counter, 
				layerId, 
				config.getConfigs().get(layerId).HAS_INCOMING, 
				config.getConfigs().get(layerId).HAS_OUTGOING, 
				config.getConfigs().get(layerId).CORTICAL_NEURONS_CONFIG,
				inhibitor, 
				position
			);
			neurons[counter++] = n;
			return n;
		}

		public CorticalNeuronFactory(MultiLayerConfig config) {
			super();
			this.config = config;
		}
		
	}

	public Brain(int totalNeuronsCount, MultiLayerConfig config) {
		super(config);
		if ( config.getNumberOfLayers()<3 ) {
			throw new RuntimeException("At least 3 layers exptected");
		}
		
		this.neurons = new CorticalNeuron[totalNeuronsCount];
		this.neuronFactory = new CorticalNeuronFactory( config );
	}

	public void build() {
		buildAndConnectLayers();		
	}
	
	// Called by superclass
	@Override
	public SphericalLayer buildLayer( LayerConfig cfg ) {
		SphericalLayer l = (SphericalLayer) new SphericalLayer(cfg)
				.populate( this.neuronFactory )
				.connectInternal();
		return l;
	}
	
	/**
     * Iterate active neurons and call consumer. Consumer returns true to keep active flag true,
     * false to clear it. This method is safe to call concurrently.
     */
    public void streamActiveNeuron(Function<CorticalNeuron, Boolean> consumer) {
        long start = System.nanoTime();
        CorticalNeuron[] snapshot = neurons; // volatile read
        if (snapshot == null) return;
        for (int i = 0; i < snapshot.length; i++) {
        	CorticalNeuron entry = snapshot[i];
            if (entry == null) continue;
            if (entry.isActive()) {
                Boolean stay = Boolean.TRUE;
                try {
                    stay = consumer.apply(entry);
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    // on exception, keep neuron active to be retried later
                    stay = Boolean.TRUE;
                }
                entry.setActive( Boolean.TRUE.equals(stay) );
            }
        }
     //   long end = System.nanoTime();
     //   processTimeNanos.add(end - start);
     //   processCounter.incrementAndGet();
    }
    
    public void streamAllNeurons(Function<CorticalNeuron, Boolean> consumer) {
    	long start = System.nanoTime();
    	CorticalNeuron[] snapshot = neurons; // volatile read
        if (snapshot == null) return;
        for (int i = 0; i < snapshot.length; i++) {
        	CorticalNeuron entry = snapshot[i];
            consumer.apply(entry);
        }
     //   long end = System.nanoTime();
     //   processTimeNanos.add(end - start);
     //   processCounter.incrementAndGet();
    }
}
