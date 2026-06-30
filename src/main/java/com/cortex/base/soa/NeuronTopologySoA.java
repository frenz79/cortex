package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;
import com.cortex.base.soa.constants.BranchTypeCode;

@SerializableClass
public final class NeuronTopologySoA {

    @SerializableAttribute
    public final int[] incomingBranchStart;

    @SerializableAttribute
    public final int[] incomingBranchCount;

    @SerializableAttribute
    public final int[] incomingBranches;   // flattened

    @SerializableAttribute
    public final int[] outgoingBranchStart;

    @SerializableAttribute
    public final int[] outgoingBranchCount;

    @SerializableAttribute
    public final int[] outgoingBranches;   // flattened

    @SerializableAttribute
    public final int[] branchGroupStart;   // per neurone per tipo (flattened index)
    
    @SerializableAttribute
    public final int[] branchGroupCount;   // per neurone per tipo

    @SerializableAttribute
    public final int[] branchGroupIndices; // flattened

    public NeuronTopologySoA(int neurons, int totalBranches, int totalGroups) {
        incomingBranchStart = new int[neurons];
        incomingBranchCount = new int[neurons];
        incomingBranches = new int[totalBranches];

        outgoingBranchStart = new int[neurons];
        outgoingBranchCount = new int[neurons];
        outgoingBranches = new int[totalBranches];

        branchGroupStart = new int[neurons * BranchTypeCode.size()];
        branchGroupCount = new int[neurons * BranchTypeCode.size()];
        branchGroupIndices = new int[totalGroups];
    }
}

