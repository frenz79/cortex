package com.cortex.base.soa.logic;

import java.util.Objects;

import com.cortex.base.soa.PlasticityParamsSoA;
import com.cortex.base.soa.PlasticitySoA;
import com.cortex.base.soa.SynapseSoA;
import com.cortex.base.utils.Maths;
import com.cortex.globals.EventBus;
import com.cortex.globals.EventBus.EventType;
import com.cortex.globals.EventBus.SynapseSpikedData;
import com.cortex.globals.EventBus.SynapseUpdatedData;

public final class SynapseLogic {

	// Monitor synapse activity
	private static final long DECAY_INTERVAL_NANOS = 50_000_000l;
	private static final int ACTIVITY_THRESHOLD = 5;
	
	// Myelinization learning rate
	private static final float ETA_MYELIN = 0.0001f;	
	private static final float MAX_MYELIN = 1.0f;
	
    private final SynapseSoA synapseSoA;
    private final PlasticitySoA plasticitySoA;
    private final PlasticityParamsSoA plasticityParamsSoA;
    private final ExcitatoryPlasticityLogic excitatoryPlasticityLogic;
    private final InhibitoryPlasticityLogic inhibitoryPlasticityLogic;
    
    public SynapseLogic(
    	ExcitatoryPlasticityLogic excitatoryPlasticityLogic, 
    	InhibitoryPlasticityLogic inhibitoryPlasticityLogic,
    	
    	SynapseSoA synapseSoA,
    	PlasticitySoA plasticitySoA,
    	PlasticityParamsSoA plasticityParamsSoA
    ) {
    	Objects.nonNull(synapseSoA);
    	Objects.nonNull(plasticitySoA);
    	
        this.synapseSoA = synapseSoA;
        this.plasticitySoA = plasticitySoA;
        this.plasticityParamsSoA = plasticityParamsSoA;
        this.excitatoryPlasticityLogic = excitatoryPlasticityLogic;
        this.inhibitoryPlasticityLogic = inhibitoryPlasticityLogic;
    }

    // --- Decay dinamico ---
    public long getTraversalTimeNanos(long now, int synId) {
        if (now - synapseSoA.lastDecayTime[synId] > DECAY_INTERVAL_NANOS) {
            synapseSoA.activityCounter[synId] *= 0.5f;
            synapseSoA.lastDecayTime[synId] = now;
        }

        float myelin = synapseSoA.myelinFactor[synId];
        long delay = (long)(synapseSoA.length[synId] * synapseSoA.baseSpeed[synId] / (1.0f + myelin));

        double sigma = delay * 0.05;
        long micro = (long)(Maths.nextGaussian() * sigma);

        return delay + micro;
    }

    private IPlasticityLogic getPlasticityLogic(int synId) {
    	return ( plasticitySoA.isExcitatory(synId) ) 
    		?this.excitatoryPlasticityLogic
    		:this.inhibitoryPlasticityLogic;
    }
    
    // --- Plasticità pre-spike ---
    public void onPreSpike(int synId, long now ) {
        synapseSoA.activityCounter[synId]++;
        
        if (getPlasticityLogic(synId).onPreSpike(
        		now, 
        		synId,
        		plasticityParamsSoA.tauPlusOrLearningRate[synId],
        		plasticityParamsSoA.tauMinusOrTargetFiringRate[synId],
        		plasticityParamsSoA.aPlusOrWMin[synId],
        		plasticityParamsSoA.aMinusOrWMax[synId]
        	)) {
            EventBus.fire(EventType.SYNAPSE_SPIKED, now, synId, SynapseSpikedData.preSpikeData());
        }        
    }

    public void onPostSpike(int synId, long postSpikeTime, long now ) {
        synapseSoA.activityCounter[synId]++; 
        if (getPlasticityLogic(synId).onPostSpike(
        	now, 
        	synId, 
        	postSpikeTime, 
        	0.0f,	// postRate
        	plasticityParamsSoA.tauPlusOrLearningRate[synId],
        	plasticityParamsSoA.tauMinusOrTargetFiringRate[synId],
        	plasticityParamsSoA.aPlusOrWMin[synId],
        	plasticityParamsSoA.aMinusOrWMax[synId]
        )) {
        	EventBus.fire(EventType.SYNAPSE_SPIKED, now, synId, SynapseSpikedData.postSpikeData());
        }
    }

    public void update(int synId, long now) {
        float oldValue = plasticitySoA.weight[synId];
        float newValue = getPlasticityLogic(synId).update(
        	now, 
        	synId, 
        	0.0f, // postRate
        	plasticityParamsSoA.homeostaticRateOrLearningRate[synId],
        	plasticityParamsSoA.tauMinusOrTargetFiringRate[synId],
        	plasticityParamsSoA.aPlusOrWMin[synId],
        	plasticityParamsSoA.aMinusOrWMax[synId],
        	plasticityParamsSoA.eligibilityDecayNanos[synId]
        );        
        
        if (oldValue != newValue) {
            EventBus.fire(EventType.SYNAPSE_UPDATED, now, synId, new SynapseUpdatedData(oldValue, newValue));
        }
    }
	
    public void applyReward(int synId, float deltaW, long now, float reward) {
    	IPlasticityLogic plast = getPlasticityLogic(synId);
    	plast.applyReward(
    		now, 
    		synId, 
    		deltaW, 
    		reward,
    		plasticityParamsSoA.aPlusOrWMin[synId],
    		plasticityParamsSoA.aMinusOrWMax[synId]);

        if (reward > 0.0f &&
            wasFrequentlyActiveInLastWindow(synId) &&
            plast.hadSignificantPairing(
            	synId,
            	plasticityParamsSoA.tauPlusOrLearningRate[synId],
            	plasticityParamsSoA.tauMinusOrTargetFiringRate[synId],
            	plasticityParamsSoA.aPlusOrWMin[synId],
            	plasticityParamsSoA.aMinusOrWMax[synId]
            )) {
            float mf = synapseSoA.myelinFactor[synId];
            mf += ETA_MYELIN * reward;
            synapseSoA.myelinFactor[synId] = Maths.clamp(mf, 0, MAX_MYELIN);
        }
    }

    public boolean wasFrequentlyActiveInLastWindow(int synId) {
        return synapseSoA.activityCounter[synId] > ACTIVITY_THRESHOLD;
    }

    public float getWeight(int synId) {
        return plasticitySoA.weight[synId];
    }

    public boolean isEligible(long now, int synId, long window ) {
        return getPlasticityLogic(synId).isEligible(now, synId, window);
    }
}


