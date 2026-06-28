package com.cortex.base.soa;

import com.cortex.base.soa.constants.BranchTypeCode;

public class NeuronTopologySoA {
	public final int[] incomingBranchStart;   // per neurone
	public final int[] incomingBranchCount;   // per neurone
	public final int[][] incomingBranchIndices; // array contiguo
    // Branch groups per neurone per tipo
    public final int[][][] branchGroups;
    
	public NeuronTopologySoA(int neurons) {
		this.incomingBranchStart = new int[neurons];
		this.incomingBranchCount = new int[neurons];
		this.incomingBranchIndices = new int[neurons][];
		
		 this.branchGroups = new int[neurons][BranchTypeCode.size()][];
	}
}
