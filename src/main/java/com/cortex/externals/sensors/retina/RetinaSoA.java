package com.cortex.externals.sensors.retina;

public final class RetinaSoA {
    public final float[] luminance;          // per pixel
    public final float[] lastLuminance;      // per pixel
    public final float[] onGain;
    public final float[] offGain;
    public final float[] threshold;
    public final int[] maxSpikesPerSample;
    public final int[] x;
    public final int[] y;
    public final int totalNeurons;

    public RetinaSoA(int totalNeurons) {
        luminance = new float[totalNeurons];
        lastLuminance = new float[totalNeurons];
        onGain = new float[totalNeurons];
        offGain = new float[totalNeurons];
        threshold = new float[totalNeurons];
        maxSpikesPerSample = new int[totalNeurons];
        x = new int[totalNeurons];
        y = new int[totalNeurons];
        this.totalNeurons = totalNeurons;
    }
}
