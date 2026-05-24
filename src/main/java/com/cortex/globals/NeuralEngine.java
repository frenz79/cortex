package com.cortex.globals;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.cortex.base.AbstractNeuron;
import com.cortex.brain.Brain;
import com.cortex.brain.CorticalNeuron;
import com.cortex.brain.layers.Layer;
import com.cortex.commons.modules.IActuator;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;
import com.cortex.commons.modules.ISupervisor;
import com.cortex.metrics.MetricsRecorder;

public class NeuralEngine {

	private static final AtomicLong GLOBAL_TIME = new AtomicLong();

	public static long now() {
		return GLOBAL_TIME.get();
	}

	public static void tick(long t) {
		GLOBAL_TIME.set(t);
	}

	private final NeuralEngineConfig config;

	private final List<ISensor> sensors;
	private final List<IActuator> actuators;
	private final List<IClassifier<?>> classifiers;
	private final List<ISupervisor<?>> supervisors;

	private final ScheduledExecutorService scheduler;

	private ScheduledFuture<?> neuronTask;
	private ScheduledFuture<?> ioTask;
	private ScheduledFuture<?> metricsRecorderTask;

	private final Brain brain;
	private MetricsRecorder metricsRecorder;

	public NeuralEngine(Brain brain, NeuralEngineConfig config ) {
		this.brain = Objects.requireNonNull(brain);
		this.config = Objects.requireNonNull(config);

		// CopyOnWriteArrayList is ideal when attaches are rare and reads are frequent
		this.sensors = new CopyOnWriteArrayList<>();
		this.actuators = new CopyOnWriteArrayList<>();
		this.classifiers = new CopyOnWriteArrayList<>();
		this.supervisors = new CopyOnWriteArrayList<>();

		ThreadFactory tf = r -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			t.setUncaughtExceptionHandler((th, ex) -> ex.printStackTrace());
			return t;
		};
		// single-thread scheduler is fine; increase pool size if tasks are heavy
		this.scheduler = Executors.newScheduledThreadPool(2, tf);
	}

	public NeuralEngine withMetricsRecorder(MetricsRecorder metricsRecorder) {
		this.metricsRecorder = metricsRecorder;
		return this;
	}

	public synchronized void start() {
		if (neuronTask != null && !neuronTask.isDone()) return; // already started

		// Neuron processing loop scheduled at fixed rate
		Runnable neuronRunnable = () -> {
			long now = System.nanoTime();
			tick(now);

			try {
				// Stream active neurons; pass the current time to each process call
				final long timestamp = now;
				final Function<CorticalNeuron, Boolean> activeNeuronsConsumer = n -> {
					try {
						return n.process(timestamp);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return false;
					} catch (Exception ex) {
						ex.printStackTrace();
						return false;
					}
				};
				brain.streamActiveNeuron(activeNeuronsConsumer);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		};

		// IO loop for sensors/actuators/classifiers/supervisors
		Runnable ioRunnable = () -> {
			long now = now();
			try {
				for (ISensor s : sensors) {
					if (s.isActive() && (now - s.getLastProcessTime()) > s.getWaitTimeNanos()) {
						s.process(now);
					}
				}
				for (IActuator a : actuators) {
					if (a.isActive() && (now - a.getLastProcessTime()) > a.getWaitTime()) {
						a.process(now);
					}
				}
				for (IClassifier<?> c : classifiers) {
					try {
						AbstractNeuron result = c.classify(now);
						if (result != null) {
							System.out.println("Classifier result:" + result);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				for (ISupervisor<?> s : supervisors) {
					try {
						s.process(now);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		};

		// schedule at fixed rate; use nanosecond precision by converting to appropriate units
		neuronTask = scheduler.scheduleAtFixedRate(
				neuronRunnable,
				0,
				config.NEURON_PERIOD_NANOS,
				TimeUnit.NANOSECONDS
				);

		ioTask = scheduler.scheduleAtFixedRate(
				ioRunnable,
				0,
				config.IO_PERIOD_NANOS,
				TimeUnit.NANOSECONDS
				);

		if (metricsRecorder!=null) {
			metricsRecorderTask = scheduler.scheduleAtFixedRate(
				() -> {
					long now = now();
					for (Layer layer : brain.getAllLayers()) {
						int layerId = layer.getLayerId();
						metricsRecorder.pollLayerStats(now, layerId);
					}
				},
				1_000,
				config.METRICS_PERIOD_NANOS, 
				TimeUnit.NANOSECONDS
				);
		}
	}

	public synchronized void stop() {
		if (neuronTask != null) neuronTask.cancel(true);
		if (ioTask != null) ioTask.cancel(true);
		if (metricsRecorderTask != null) metricsRecorderTask.cancel(true);

		scheduler.shutdownNow();
		try {
			if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
				System.err.println("Scheduler did not terminate in time");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void attachSensor(ISensor s) {
		this.sensors.add(Objects.requireNonNull(s));
	}

	public void attachActuator(IActuator a) {
		this.actuators.add(Objects.requireNonNull(a));
	}

	public void attachClassifier(IClassifier<?> c) {
		this.classifiers.add(Objects.requireNonNull(c));
	}

	public void attachSupervisor(ISupervisor<?> s) {
		this.supervisors.add(Objects.requireNonNull(s));
	}
}
