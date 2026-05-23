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
import com.cortex.brain.layers.SphericalLayer;
import com.cortex.commons.modules.IActuator;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;
import com.cortex.commons.modules.ISupervisor;
import com.cortex.metrics.LayerStats;
import com.cortex.metrics.MetricsRecorder;

public class NeuralEngine {

	private static final AtomicLong GLOBAL_TIME = new AtomicLong();

	public static long now() {
		return GLOBAL_TIME.get();
	}

	public static void tick(long t) {
		GLOBAL_TIME.set(t);
	}

	private final List<ISensor> sensors;
	private final List<IActuator> actuators;
	private final List<IClassifier<?>> classifiers;
	private final List<ISupervisor<?>> supervisors;

	private final ScheduledExecutorService scheduler;

	// scheduling parameters (tunable)
	private static final long NEURON_PERIOD_NANOS = TimeUnit.MICROSECONDS.toNanos(200); // 200 µs
	private static final long IO_PERIOD_NANOS = TimeUnit.MILLISECONDS.toNanos(1); // 1 ms

	private ScheduledFuture<?> neuronTask;
	private ScheduledFuture<?> ioTask;
	private ScheduledFuture<?> stabilizerTask;
	
	private final Brain brain;
	private final AdaptiveStabilizer stabilizer;
	
	public NeuralEngine(Brain brain, AdaptiveStabilizer stabilizer) {
		this.brain = Objects.requireNonNull(brain);
		this.stabilizer = stabilizer;
		
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
				NEURON_PERIOD_NANOS,
				TimeUnit.NANOSECONDS
				);

		ioTask = scheduler.scheduleAtFixedRate(
				ioRunnable,
				0,
				IO_PERIOD_NANOS,
				TimeUnit.NANOSECONDS
				);


		stabilizerTask = scheduler.scheduleAtFixedRate(
		        stabilizer::step,
		        1_000,      // delay iniziale
		        500,        // ogni 500 ms
		        TimeUnit.MILLISECONDS
		);
		
		// scheduled reporter already present; keep using scheduler
		scheduler.scheduleAtFixedRate(() -> {
			try {
				System.out.println("== Avg Process Time:" + GlobalContext.getAverageProcessTimeMillis() + "ms ===========");
				System.out.println(
						  "L" 
						+ " | NEURONS" 
						+ " | ACT"
						+ " | SYN_W"
						+ " | SYN_W_STD" 
						+ " | FIRE_ACT" 
						+ " | FIRE_ALL" 
						+ " | SAT_MAX" 
						+ " | SAT_MIN"
						+ " | SPARSE" 
						+ " | PLAST" 
						+ " | ENERGY" 
						);
				
				for (SphericalLayer l : brain.getAllLayers()) {
					LayerStats stats = GlobalContext.getAndResetStats(l.getLayerId());
					if (stats != null) {
						System.out.println(
								l.getLayerId() 
								+ " | " + stats.totalNeurons() 
								+ " | " + stats.activeNeurons()
								+ " | " + String.format("%,.2f",stats.averageSynapticWeight() )
								+ " | " + String.format("%,.2f",stats.synapticWeightStdDev() )
								+ " | " + String.format("%,.2f",stats.avgFiringRateActive() )
								+ " | " + String.format("%,.2f",stats.avgFiringRateAll() )
								+ " | " + String.format("%,.2f",stats.saturatedMaxRatio() )
								+ " | " + String.format("%,.2f",stats.saturatedMinRatio() )
								+ " | " + String.format("%,.2f",stats.sparsity() )
								+ " | " + String.format("%,.2f",stats.totalPlasticity())
								+ " | " + String.format("%,.2f",stats.energy() )
								);
						
					} else {
						System.out.println("Layer:" + l.getLayerId() + " NO STATS");
					}
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}, 3, 3, TimeUnit.SECONDS);
	}

	public synchronized void stop() {
		if (neuronTask != null) neuronTask.cancel(true);
		if (ioTask != null) ioTask.cancel(true);
		if (stabilizerTask != null) stabilizerTask.cancel(true);
		
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
