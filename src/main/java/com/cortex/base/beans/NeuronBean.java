package com.cortex.base.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cortex.base.utils.Point3f;

public final class NeuronBean {

    public final int id;
    public final int layerId;
    public final int hemisphereId;
	
    public final float bias;
    public final float threshold;

    public final Point3f position;
    public final boolean inhibitor;
	
    // branchId → BranchBean
    public final Map<Integer, BranchBean> branches = new HashMap<>();

    public final List<SynapseBean> outgoing = new ArrayList<>();
    public final List<SynapseBean> incoming = new ArrayList<>();

    public NeuronBean(int id, int layerId, int hemisphereId, float bias, float threshold, boolean inhibitor, Point3f position) {
        this.id = id;
        this.layerId = layerId;
        this.hemisphereId = hemisphereId;
        this.bias = bias;
        this.threshold = threshold;
        this.position = position;
        this.inhibitor = inhibitor;
    }
}
