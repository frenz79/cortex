public class Lidar implements ISensor {

    private static final String SENSOR_ID = "LIDAR";

    private final LidarConfig config;
    private final LidarNeuron[] neurons;

    private volatile float[] distances; // input dal world

    private volatile boolean active;
    private volatile long lastProcessTime = 0;

    public LidarSensor(LidarConfig config, LidarNeuronConfig neuronConfig) {
        this.config = config;
        this.neurons = new LidarNeuron[config.RAYS];

        for (int i = 0; i < config.RAYS; i++) {
            neurons[i] = new LidarNeuron(i, neuronConfig);
        }
    }

    @Override
    public boolean process(long currTimeNanos) {

        this.lastProcessTime = currTimeNanos;

        if (!active || distances == null) {
            return true;
        }

        for (int i = 0; i < config.RAYS; i++) {

            float d = normalize(distances[i]);

            int c = neurons[i].process(currTimeNanos, d);

            if (c > 0) {
                List<Spike> spikes = neurons[i].drainSpikes();
                for (Spike s : spikes) {
                    neurons[i].fire(s);
                }
            }
        }

        return true;
    }

    private float normalize(float distance) {
        // distanza → [0..1], invertita (vicino = 1)
        float v = 1f - (distance / config.MAX_DISTANCE);
        return Maths.clamp(v, 0f, 1f);
    }

    // --------------------------------------------------
    // INPUT

    public void setDistances(float[] distances) {
        this.distances = distances;
    }

    // --------------------------------------------------
    // ISensor impl

    @Override
    public long getWaitTimeNanos() {
        return config.SAMPLING_PERIOD_NANOS;
    }

    @Override
    public long getLastProcessTime() {
        return lastProcessTime;
    }

    @Override
    public String getId() {
        return SENSOR_ID;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void stop() {
        this.active = false;
    }

    @Override
    public void start() {
        this.active = true;
    }

    @Override
    public AbstractNeuron[][] getNeurons() {
        // coerente con retina → 2D, ma qui 1xN
        AbstractNeuron[][] out = new AbstractNeuron[config.RAYS][1];
        for (int i = 0; i < config.RAYS; i++) {
            out[i][0] = neurons[i];
        }
        return out;
    }

    @Override
    public int getSynapsesCount() {
        int ret = 0;
        for (int i = 0; i < config.RAYS; i++) {
            ret += neurons[i].getOutSynapses().size();
        }
        return ret;
    }
}
