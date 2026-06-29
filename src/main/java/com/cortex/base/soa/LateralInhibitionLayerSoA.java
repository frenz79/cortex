package com.cortex.base.soa;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableClass;

@SerializableClass
public class LateralInhibitionLayerSoA {
	// blending between strategies
	@SerializableAttribute
    public final float[] topoWeight;   // per layer
	@SerializableAttribute
    public final float[] synWeight;    // per layer

    // strategies strength
	@SerializableAttribute
    public final float[] topoStrength; // per layer
	@SerializableAttribute
    public final float[] synStrength;  // per layer

    // additional params
	@SerializableAttribute
    public final float[] topoRadius;       // per layer
	@SerializableAttribute
    public final float[] synActivityScale; // per layer

    public LateralInhibitionLayerSoA(int totalLayers) {
        topoWeight = new float[totalLayers];
        synWeight = new float[totalLayers];
        topoStrength = new float[totalLayers];
        synStrength = new float[totalLayers];
        topoRadius = new float[totalLayers];
        synActivityScale = new float[totalLayers];
    }
}
