package com.cortex.externals.sensors.lidar;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.externals.AbstractExternalNeuron;
import com.cortex.base.utils.Maths;

public class LidarNeuron extends AbstractExternalNeuron {

    private float lastDistance = Float.NaN;

    private final Queue<Float> spikesAmplitude = new ConcurrentLinkedQueue<>();

    private final LidarNeuronConfig config;

    private float firingRate;
    private long lastUpdate;

    public LidarNeuron(NeuronsStateBuff neuronsStates, int index, LidarNeuronConfig config) {
        super( neuronsStates, index, false, true );
        this.config = config;
    }

    public int process(long currTimeNanos, float distance) {

        // inizializzazione
        if (Float.isNaN(lastDistance)) {
            lastDistance = distance;
            return 0;
        }

        float delta = lastDistance - distance;
        // ATTENZIONE: invertito rispetto alla retina
        // distanza ↓ → oggetto si avvicina → spike eccitatorio

        float amplitude = 0f;

        if (delta > config.THRESHOLD) {
            // oggetto si avvicina
            amplitude = delta * config.APPROACH_GAIN;
        } else if (delta < -config.THRESHOLD) {
            // oggetto si allontana
            amplitude = -delta * config.RECEDE_GAIN;
        }

        int spikeCount = 0;

        if (amplitude > 0f) {

            float raw = amplitude * config.SPIKE_SCALING;

            spikeCount = (int) raw;

            float fractional = raw - spikeCount;
            if (ThreadLocalRandom.current().nextFloat() < fractional)
                spikeCount++;

            spikeCount = Maths.min(spikeCount, config.MAX_SPIKES_PER_SAMPLE);

            for (int i = 0; i < spikeCount; i++) {
            	spikesAmplitude.add(amplitude);
            }
        }

        lastDistance = distance;
        firingRate += spikeCount;

        return spikeCount;
    }

    public List<Float> drainSpikes() {
        List<Float> out = new ArrayList<>(spikesAmplitude);
        spikesAmplitude.clear();
        return out;
    }

    @Override
    public float getRecentFiringRate(long now) {
        long dt = now - lastUpdate;
        firingRate *= Math.exp(-dt / config.TAU);
        lastUpdate = now;
        return firingRate;
    }
}
