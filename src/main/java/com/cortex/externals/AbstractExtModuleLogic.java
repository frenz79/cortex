package com.cortex.externals;

import java.util.concurrent.atomic.AtomicBoolean;

import com.cortex.base.beans.NeuronBean;
import com.cortex.base.soa.constants.DirectionCode;
import com.cortex.base.soa.logic.SpikeRingBufferLogic;
import com.cortex.base.utils.Point3f;

public abstract class AbstractExtModuleLogic {

	private final String moduleId;
	private final long waitTimeNanos;
	private final int targetLayerId;
	private final int targetHemisphereId;
	
	private long lastProcessTime;
	private final AtomicBoolean active = new AtomicBoolean(false);
	
	private final SpikeRingBufferLogic spikeBufferLogic;
	private ExtSynapseSoA synapseSoA;
	private ExtNeuronSoA neuronSoA;
	
	private NeuronBean[] neurons;
	
	protected abstract void processInternal(long now);
	protected abstract int getDirection();
	
	public AbstractExtModuleLogic(String moduleId, long waitTimeNanos, int targetLayerId, int targetHemisphereId, SpikeRingBufferLogic spikeBufferLogic) {
		super();
		this.moduleId = moduleId;
		this.waitTimeNanos = waitTimeNanos;
		this.targetLayerId = targetLayerId;
		this.targetHemisphereId = targetHemisphereId;
		this.spikeBufferLogic = spikeBufferLogic;
	}
	
	public void process(long now) {
		if (now-lastProcessTime<waitTimeNanos || !isActive()) {
			return;
		}
		this.lastProcessTime = now;
		processInternal(now);
	}

	public void fire(long now, int idx, float amplitude, boolean inhibitory) {
		int sStart = synTopologySoA.synapseStart[idx];
		int sCount = synTopologySoA.synapseCount[idx];

		for (int si = sStart; si < sStart + sCount; si++) {
		    int synId = synTopologySoA.synapseIndex[si];

		    spikeBufferLogic.addSpike(
		        now,
		        synId,
		        amplitude,
		        inhibitory,
		        now
		    );
		}
	}

	public Point3f getPluginSite() {
		return new Point3f(0.0f,0.0f,0.0f);
	}
	
	public int getTargetHemisphereId() {
		return targetHemisphereId;
	}
	
	public int getTargetLayerId() {
		return targetLayerId;
	}
	
	public long getLastProcessTime() {
		return lastProcessTime;
	}

	public boolean isActive() {
		return active.get();
	}

	public void enable() {
		this.active.set(true);
	}
	
	public void disable() {
		this.active.set(false);
	}
	
	public long getWaitTimeNanos() {
		return waitTimeNanos;
	}
	
	public String getModuleId() {
		return moduleId;
	}
	
	public int getSynapsesCount() {
		return synapseSoA.totalSynapses;
	}

	public int getNeuronsCount() {
		return neurons.length;
	}
	
	public NeuronBean[] getNeurons() {
		return neurons;
	}

	public ExtSynapseSoA getSynapseSoA() {
		return synapseSoA;
	}

	public void setSynapseSoA(ExtSynapseSoA synapseSoA) {
		this.synapseSoA = synapseSoA;
	}

	public ExtNeuronSoA getNeuronSoA() {
		return neuronSoA;
	}

	public void setNeuronSoA(ExtNeuronSoA neuronSoA) {
		this.neuronSoA = neuronSoA;
	}
}
