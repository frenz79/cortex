package com.cortex.globals;

import java.util.concurrent.TimeUnit;

public class NeuralEngineConfig {

	public long NEURON_PERIOD_NANOS = TimeUnit.MICROSECONDS.toNanos(10); // 200 µs
	public long IO_PERIOD_NANOS = TimeUnit.MILLISECONDS.toNanos(1); // 1 ms
	public long METRICS_PERIOD_NANOS = TimeUnit.MILLISECONDS.toNanos(500); // 500 ms
	
}
