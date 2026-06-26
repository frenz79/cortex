package com.cortex.base.soa;

import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.SynapseBranch.Direction;

public final class SynapseBranchStateSoA {

	// Branch dynamic state (SoA)
    public float[] branchPotential;	// Weighted input sum
    public float[] branchActivity;	// Recent activity (for decay)
    public float[] inhibition;		// Lateral inhibition level
    public float[] gain;			// Branch modulator
    public boolean[] active;		// A synapse has spikes to process
    public long[] lastProcessTime;
    int[] spikeCount;
    
    // Topology (SoA)
    public int[] synapseStart;   // indice iniziale nel vettore globale delle sinapsi
    public int[] synapseCount;   // numero di sinapsi nel ramo
    public BranchType[] type;
    public Direction[] direction;

    public SynapseBranchStateSoA(int totalBranches) {
        branchPotential = new float[totalBranches];
        branchActivity  = new float[totalBranches];
        inhibition      = new float[totalBranches];
        gain            = new float[totalBranches];
        active          = new boolean[totalBranches];
        lastProcessTime = new long[totalBranches];

        synapseStart    = new int[totalBranches];
        synapseCount    = new int[totalBranches];
        type            = new BranchType[totalBranches];
        direction       = new Direction[totalBranches];
    }
}