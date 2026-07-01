package com.cortex.base.layers;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.beans.NeuronBean;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Point3f;

public abstract class Abstract3DLayer {

	final Logger logger = LogManager.getLogger(this.getClass());

	protected final LayerConfig config;
	private NeuronBean[] neurons;
	private int neuronsStart;
	private int neuronsLen;
	private int synapsesCount = 0;
	private SpatialHash spatialHash;
	
	public Abstract3DLayer(LayerConfig config) {
		super();
		this.config = config;
	}
	
	protected abstract Point3f getPosition( int i );

	public void populate( NeuronBean[] neurons, int start, int len ) {
		long startTime = System.nanoTime();
		this.neuronsStart = start;
		this.neuronsLen = len;
		
		for(int i=0 ;i<len; i++) {
			neurons[i+start] = new NeuronBean(
				i+start,
				getLayerId(), 
				getHemisphereId(), 
				isInhibitor(), 
				getPosition(i));
		}
		this.spatialHash = new SpatialHash(neurons, config.DIMENSION / 2.0f );
		
		long endTime = System.nanoTime();
		logger.info("L{} generated {} neurons in {} micros",
			getLayerId(),
			getNeuronsCount(),
			TimeUnit.NANOSECONDS.toMicros(endTime-startTime)
		);
	}

	public LayerConfig getConfig() {
		return config;
	}

	public int getLayerId() {
		return config.getLayerId();
	} 
	
	public int getHemisphereId() {
		return config.getHemisphereId();
	} 

	protected boolean randomBoolean( float trueProbability ) {
		return ThreadLocalRandom.current().nextFloat(0.0f, 1.0f)<=trueProbability;
	}

	protected boolean isInhibitor( ) {
		return randomBoolean( config.INHIBITOR_FREQ );
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

	public NeuronBean[] getNeurons() {
		return neurons;
	}

	public int getNeuronsStart() {
		return neuronsStart;
	}

	public int getNeuronsLen() {
		return neuronsLen;
	}
}
