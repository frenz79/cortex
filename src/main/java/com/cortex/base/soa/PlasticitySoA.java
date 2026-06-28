package com.cortex.base.soa;

public class PlasticitySoA {
	public final float[] weight;
    public final float[] eligibility;

    public final long[] lastEligibilityUpdate;
    public final long[] lastHomeostasisUpdate;

    public final long[] lastPreSpike;
    public final long[] lastPostSpike;
    
    // bit 0 : enabled (0/1)
    // bit 1 : plasticityType (0 = exc, 1 = inh)
    // bit 2..7 : reserved for future use
    public final byte[] flags;
    
    public PlasticitySoA(int totalSynapses) {
    	weight      = new float[totalSynapses];
    	eligibility = new float[totalSynapses];
    	lastEligibilityUpdate = new long[totalSynapses];
    	lastHomeostasisUpdate = new long[totalSynapses];
    	lastPreSpike  = new long[totalSynapses];
    	lastPostSpike = new long[totalSynapses];
    	flags = new byte[totalSynapses];
    }
    
    public void setEnabled( int synId ) {
    	flags[synId] |= 0b00000001; // set bit 0
    }
    
    public void setDisabled( int synId ) {
    	flags[synId] &= 0b11111110; // clear bit 0
    }
      
    public boolean isEnabled( int synId ) {
    	return (flags[synId] & 0b00000001) != 0;
    }
    
    public void setPlasticityType(int synId, boolean isExcitatory) {
    	if (isExcitatory) {
    		flags[synId] &= 0b11111101;
    	} else {    	   
    	    flags[synId] |= 0b00000010;
    	}
    }
    
    public boolean isExcitatory(int synId) {
    	return (flags[synId] & 0b00000010) == 0;
    }
    
    public boolean isInhibitory (int synId) {
    	return (flags[synId] & 0b00000010) != 0;
    }
}
