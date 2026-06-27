package com.cortex.base.soa;

public class PlasticitySoA {
	public float[] weight;
    public float[] eligibility;

    public long[] lastEligibilityUpdate;
    public long[] lastHomeostasisUpdate;

    public long[] lastPreSpike;
    public long[] lastPostSpike;
    
    // bit 0 : enabled (0/1)
    // bit 1 : plasticityType (0 = exc, 1 = inh)
    // bit 2..7 : reserved for future use
    public byte[] flags;
    
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
