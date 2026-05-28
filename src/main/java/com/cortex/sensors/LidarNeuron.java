
public class LidarNeuron extends AbstractNeuron {

    private float lastDistance = Float.NaN;

    private final Queue<Spike> spikes = new ConcurrentLinkedQueue<>();

    private final LidarNeuronConfig config;

    private float firingRate;
    private long lastUpdate;

    public LidarNeuron(int index, LidarNeuronConfig config) {
        super(
                -1,
                index,
                false,  // non hidden
                true,   // input neuron
                false,
                null
        );
        this.config = config;
    }

    @Override
    public boolean process(long currTimeNanos) {
        return true;
    }

    public int process(long currTimeNanos, float distance) {

        // inizializzazione
        if (Float.isNaN(lastDistance)) {
            lastDistance = distance;
            return 0;
        }

        float delta = lastDistance - distance;
        // 🔥 ATTENZIONE: invertito rispetto alla retina
        // distanza ↓ → oggetto si avvicina → spike eccitatorio

        float amplitude = 0f;
        boolean inhibitory = false;

        if (delta > config.THRESHOLD) {
            // ✅ oggetto si avvicina
            amplitude = delta * config.APPROACH_GAIN;
            inhibitory = false;

        } else if (delta < -config.THRESHOLD) {
            // ✅ oggetto si allontana
            amplitude = -delta * config.RECEDE_GAIN;
            inhibitory = true;
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
                spikes.add(new Spike(amplitude, currTimeNanos, inhibitory));
            }
        }

        lastDistance = distance;
        firingRate += spikeCount;

        return spikeCount;
    }

    public List<Spike> drainSpikes() {
        List<Spike> out = new ArrayList<>(spikes);
        spikes.clear();
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
