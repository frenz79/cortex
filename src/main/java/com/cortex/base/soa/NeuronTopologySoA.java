package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;
import com.cortex.base.soa.constants.BranchTypeCode;

@SerializableClass
public class NeuronTopologySoA {
	@SerializableAttribute
	public final int[] incomingBranchStart;   // per neurone
	@SerializableAttribute
	public final int[] incomingBranchCount;   // per neurone
	@SerializableAttribute
	public final int[][] incomingBranchIndices; // array contiguo
    // Branch groups per neurone per tipo
	@SerializableAttribute
    public final int[][][] branchGroups;
    
	public NeuronTopologySoA(int neurons) {
		this.incomingBranchStart = new int[neurons];
		this.incomingBranchCount = new int[neurons];
		this.incomingBranchIndices = new int[neurons][];
		
		 this.branchGroups = new int[neurons][BranchTypeCode.size()][];
	}
}
