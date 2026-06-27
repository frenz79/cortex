package com.cortex.base.soa;

public class LateralInhibitionLayerSoA {
	 // blending between strategies
    public float[] topoWeight;   // per layer
    public float[] synWeight;    // per layer

    // strategies strength
    public float[] topoStrength; // per layer
    public float[] synStrength;  // per layer

    // parametri aggiuntivi
    public float[] topoRadius;       // per layer
    public float[] synActivityScale; // per layer

    public LateralInhibitionLayerSoA(int totalLayers) {
        topoWeight = new float[totalLayers];
        synWeight = new float[totalLayers];
        topoStrength = new float[totalLayers];
        synStrength = new float[totalLayers];
        topoRadius = new float[totalLayers];
        synActivityScale = new float[totalLayers];
    }
}
