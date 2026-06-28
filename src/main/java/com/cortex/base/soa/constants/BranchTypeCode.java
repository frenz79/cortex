package com.cortex.base.soa.constants;

public final class BranchTypeCode {		
	public static final int NEAR = 0;
    public static final int FAR = 1;
    public static final int FEEDFORWARD = 2;
    public static final int FEEDBACK = 3;
    public static final int EXTERNAL = 4;
    
    private static final int[] BRANCH_TYPES = new int[] {
    	NEAR, FAR, FEEDFORWARD, FEEDBACK, EXTERNAL 	
    };
    
    public static int size() {
    	return 5;
    }
    
    public static int[] getBranchTypes() {
    	return BRANCH_TYPES;
    }
}