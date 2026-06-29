package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;
import com.cortex.base.soa.constants.BranchTypeCode;
import com.cortex.base.soa.constants.DirectionCode;

@SerializableClass
public final class SynapseBranchSoA {

	// dynamic state
	@SerializableAttribute
    public final float[] branchPotential;
	@SerializableAttribute
    public final float[] branchActivity;
	@SerializableAttribute
    public final float[] inhibition;
	@SerializableAttribute
    public final float[] gain;
	@SerializableAttribute
    public final long[] lastProcessTime;
	@SerializableAttribute
    public final int[] spikeCount;

    // topology
	@SerializableAttribute
    public final int[] synapseStart;     // full int
	@SerializableAttribute
    public final byte[] synapseCount;    // 0–255
    
    // bit 0      → active
    // bits 1..3  → branchType (0–7)   // 5 types → fits in 3 bits
    // bit 4      → direction (0–1)    // INCOMING / OUTGOING
    // bits 5..7  → reserved
	@SerializableAttribute
    public byte[] topoFlags;
	
	public final int totalBranches;       

    public SynapseBranchSoA(int totalBranches) {
        branchPotential = new float[totalBranches];
        branchActivity  = new float[totalBranches];
        inhibition      = new float[totalBranches];
        gain            = new float[totalBranches];
        lastProcessTime = new long[totalBranches];
        spikeCount      = new int[totalBranches];

        synapseStart    = new int[totalBranches];
        synapseCount    = new byte[totalBranches];
        topoFlags       = new byte[totalBranches];
        this.totalBranches = totalBranches;
    }

    // --- active flag (bit 0) ---
    public void setActive(int bId) {
        topoFlags[bId] |= 0b00000001;
    }

    public void clearActive(int bId) {
        topoFlags[bId] &= 0b11111110;
    }

    public boolean isActive(int bId) {
        return (topoFlags[bId] & 0b00000001) != 0;
    }

    public void setActive(int bId, boolean active) {
    	if (active)
    		setActive(bId);
    	else
    		clearActive(bId);
	}

    // --- BranchType (bits 1..3) ---
    // bits 1..3 = branchType
    public void setBranchType(int bId, int typeCode) {
        topoFlags[bId] = (byte)((topoFlags[bId] & 0b11110001) | ((typeCode & 0b111) << 1));
    }

    public int getBranchTypeCode(int bId) {
        return (topoFlags[bId] >> 1) & 0b111;
    }
    
    public boolean isExternalBranchType(int bId) {
    	return ((topoFlags[bId] >> 1) & 0b111) == BranchTypeCode.EXTERNAL;
    }
    
    public boolean isNearBranchType(int bId) {
    	return ((topoFlags[bId] >> 1) & 0b111) == BranchTypeCode.NEAR;
    }
    
    public boolean isFarBranchType(int bId) {
    	return ((topoFlags[bId] >> 1) & 0b111) == BranchTypeCode.FAR;
    }
    
    public boolean isFeedFwdBranchType(int bId) {
    	return ((topoFlags[bId] >> 1) & 0b111) == BranchTypeCode.FEEDFORWARD;
    }
    
    public boolean isFeedBackBranchType(int bId) {
    	return ((topoFlags[bId] >> 1) & 0b111) == BranchTypeCode.FEEDBACK;
    }

    // --- Direction (bit 4) ---
    public void setDirection(int bId, int dirCode) {
        topoFlags[bId] = (byte)((topoFlags[bId] & 0b11101111) | ((dirCode & 1) << 4));
    }
    
    public int getDirectionCode(int bId) {
        return (topoFlags[bId] >> 4) & 1;
    }
    
    public boolean isIncoming(int bId) {
        return ((topoFlags[bId] >> 4) & 1) == DirectionCode.INCOMING;
    }

    public boolean isOutgoing(int bId) {
        return ((topoFlags[bId] >> 4) & 1) == DirectionCode.OUTGOING;
    }
}
