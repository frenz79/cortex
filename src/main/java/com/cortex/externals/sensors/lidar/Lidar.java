package com.cortex.externals.sensors.lidar;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.AbstractNeuron.NeuronsStateBuff;
import com.cortex.base.externals.ExternalConnConfig;
import com.cortex.base.externals.ISensor;
import com.cortex.base.utils.Maths;

public class Lidar extends ISensor {

    private static final String SENSOR_ID = "LIDAR";

    private final LidarConfig config;
    private final LidarNeuron[] neurons;
    private final NeuronsStateBuff neuronsStates;
    
    private volatile float[] distances; // input dal world

    public Lidar(LidarConfig config, LidarNeuronConfig neuronConfig) {
        this.config = config;
        this.neurons = new LidarNeuron[config.RAYS];
        this.neuronsStates = new NeuronsStateBuff(config.RAYS );
        
        for (int i = 0; i < config.RAYS; i++) {
            neurons[i] = new LidarNeuron(neuronsStates, i, neuronConfig);
        }
    }

    @Override
    public boolean processExt(long now) throws InterruptedException {
        if (distances == null) {
            return true;
        }
        /*
        for (int i = 0; i < config.RAYS; i++) {
            float d = normalize(distances[i]);
            int c = neurons[i].process(now, d);
            if (c > 0) {
                List<Float> spikesAmplitude = neurons[i].drainSpikes();
                for (Float amplitude : spikesAmplitude) {
                	for (SynapseBranch sb : neurons[i].getOutSynapseBranches()) {
                		for ( Synapse s : sb.synapses ) {
                			// No real delay, "ideal source"
        					s.addSpike(
        						Spike.createWithJitter(amplitude, (amplitude<0), now)
        					);
                		}
                	}                	
				}
            }
        }
        */
        return true;
    }
    
    private float normalize(float distance) {
        // distanza → [0..1], invertita (vicino = 1)
        float v = 1f - (distance / config.MAX_DISTANCE);
        return Maths.clamp(v, 0f, 1f);
    }

    // --------------------------------------------------
    // INPUT

    public void setDistances(float[] distances) {
        this.distances = distances;
    }

    // --------------------------------------------------
    // ISensor impl

    @Override
    public long getWaitTimeNanos() {
        return config.SAMPLING_PERIOD_NANOS;
    }

    @Override
    public String getId() {
        return SENSOR_ID;
    }

    @Override
    public AbstractNeuron[][] getNeurons() {
        // coerente con retina → 2D, ma qui 1xN
        AbstractNeuron[][] out = new AbstractNeuron[config.RAYS][1];
        for (int i = 0; i < config.RAYS; i++) {
            out[i][0] = neurons[i];
        }
        return out;
    }

    @Override
	public ExternalConnConfig getExternalConnConfig() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isProducer() {
		return true;
	}
}
