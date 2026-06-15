package com.cortex.base.lateral_inhibition;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.SynapseBranch;
import com.cortex.base.SynapseBranch.BranchType;
import com.cortex.base.layers.Abstract3DLayer;
import com.cortex.base.utils.IntList;
import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

public class TopographicLateralInhibition implements ILateralInhibitionStrategy {

	private final float LI_STRENGTH;
	
	public TopographicLateralInhibition(float LI_STRENGTH) {
		this.LI_STRENGTH = LI_STRENGTH;
	}
	
	@Override
	public void updateInhibition(long deltaTimeNanos, AbstractNeuron firingNeuron, float gain) {
		Abstract3DLayer layer = firingNeuron.getLayer();
		Point3f position = firingNeuron.getPosition();
		
	    IntList neighbors = layer.getSpatialHash().getSpatialHashCell(firingNeuron);

	    for (int idx : neighbors.getData()) {
	    	AbstractNeuron n = layer.getNeurons()[idx];
	    	
	        if (n == firingNeuron || n.isInhibitor()) continue;
	        
	        float dist = position.distance(n.getPosition());
	        // When close to the edge distance may get > cellSize
	        float norm = Maths.clamp(1.0f - dist / layer.getSpatialHash().getCellSize(), 0f, 1f);
	        float inhib = LI_STRENGTH * gain *norm;
	        
	        for (SynapseBranch b : n.getInSynapseBranches()) {
	        	// do not inhibite sensors
	            if (b.type == BranchType.EXTERNAL) continue;
	            b.inhibition += inhib;
	        }
	    }
	}
}
