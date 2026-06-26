package com.cortex.base.layers;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.SpatialHash;
import com.cortex.brain.Hemisphere.CorticalNeuronFactory;

public abstract class Abstract3DLayer {

	final Logger logger = LogManager.getLogger(this.getClass());

	protected final LayerConfig config;
	protected AbstractNeuron[] neurons;
	private int synapsesCount = 0;
	private SpatialHash spatialHash;
	
	public Abstract3DLayer(LayerConfig config) {
		super();
		this.config = config;
	}
	
	protected abstract Abstract3DLayer internalPopulate(CorticalNeuronFactory neuronFactory );

	public Abstract3DLayer populate( CorticalNeuronFactory neuronFactory ) {
		long startTime = System.nanoTime();
		this.neurons = new AbstractNeuron[getNeuronsCount()];
		
		internalPopulate(neuronFactory);
		this.spatialHash = new SpatialHash(getNeurons(), config.DIMENSION / 2.0f );
		
		long endTime = System.nanoTime();
		logger.info("L{} generated {} neurons in {} micros",
			getLayerId(),
			getNeuronsCount(),
			TimeUnit.NANOSECONDS.toMicros(endTime-startTime)
		);
		
		return this;
	}

	public IntList getSpatialHashCell( AbstractNeuron n ) {
		return this.spatialHash.getSpatialHashCell(n);
	}
	
	public LayerConfig getConfig() {
		return config;
	}

	public int getLayerId() {
		return config.getLayerId();
	} 

	protected boolean randomBoolean( float trueProbability ) {
		return ThreadLocalRandom.current().nextFloat(0.0f, 1.0f)<=trueProbability;
	}

	protected boolean isInhibitor( ) {
		return randomBoolean( config.INHIBITOR_FREQ );
	}

	public AbstractNeuron[] getNeurons() {
		return neurons;
	}

	public int getNeuronsCount() {
		return config.NEURONS_COUNT;
	}

	public SpatialHash getSpatialHash() {
		return spatialHash;
	}

	public int getSynapsesCount() {
		return synapsesCount;
	}

	public void setSynapsesCount(int synapsesCount) {
		this.synapsesCount = synapsesCount;
	}
}
