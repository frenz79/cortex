package com.cortex.externals;

import java.util.concurrent.atomic.AtomicBoolean;

import com.cortex.base.soa.logic.SpikeRingBufferLogic;

public abstract class AbstractExternalModuleLogic {

	private final String moduleId;
	private final long waitTimeNanos;
	private final int targetLayerId;
	private final int targetHemisphereId;
	
	private long lastProcessTime;
	private final AtomicBoolean active = new AtomicBoolean(false);
	
	private final SpikeRingBufferLogic spikeBufferLogic;
	private ExternalModuleSynTopologySoA synTopologySoA;
	
	protected abstract void processInternal(long now);
	
	public AbstractExternalModuleLogic(String moduleId, long waitTimeNanos, int targetLayerId, int targetHemisphereId, SpikeRingBufferLogic spikeBufferLogic) {
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

	public ExternalModuleSynTopologySoA getSynTopologySoA() {
		return synTopologySoA;
	}

	public void setSynTopologySoA(ExternalModuleSynTopologySoA synTopologySoA) {
		this.synTopologySoA = synTopologySoA;
	}
	
	public int getSynapsesCount() {
		return synTopologySoA.getTotalSynapses();
	}
}
