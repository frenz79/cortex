package com.cortex.base.beans;

public final class BranchBean {
    public final int id;
    public final int type;	// BranchTypeCode
    public final float attenuation;   // 0..1
    public final float plasticityGain;

    public BranchBean(int id, int type, float attenuation, float plasticityGain) {
        this.id = id;
        this.type = type;
        this.attenuation = attenuation;
        this.plasticityGain = plasticityGain;
    }
}
