package com.cortex.base.soa;

public class LateralInhibitionLayerSoA {
	 // blending between strategies
    public final float[] topoWeight;   // per layer
    public final float[] synWeight;    // per layer

    // strategies strength
    public final float[] topoStrength; // per layer
    public final float[] synStrength;  // per layer

    // parametri aggiuntivi
    public final float[] topoRadius;       // per layer
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
