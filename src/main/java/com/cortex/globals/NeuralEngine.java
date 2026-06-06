package com.cortex.globals;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.brain.Brain;
import com.cortex.brain.layers.Layer;
import com.cortex.commons.modules.IActuator;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;
import com.cortex.commons.modules.ISupervisor;
import com.cortex.metrics.MetricsRecorder;

public class NeuralEngine {

	private final Logger logger = LogManager.getLogger(this.getClass());

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

	private Thread neuronThread;
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

		// single-thread scheduler is fine; increase pool size if tasks are heavy
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
	}

	public NeuralEngine withMetricsRecorder(MetricsRecorder metricsRecorder) {
		this.metricsRecorder = metricsRecorder;
		return this;
	}
	   
	public synchronized void start() {
		if (neuronThread != null ) return; // already started
		
		logger.info("Engine started!");
		
		LongAdder processCounter = new LongAdder();
		LongAdder processTimeNanos = new LongAdder();
		
		Thread neuronThread = new Thread(() -> {
			logger.info("neuronThread started!");
			long localCounter = 0l;
			long localTime = 0l;
		    while (!Thread.currentThread().isInterrupted()) {
		        long now = System.nanoTime();
		        tick(now);
				localCounter++;

		        brain.streamActiveNeuron(n -> {
					try {
						return n.process(now);
					} catch (Exception ex) {
						Thread.currentThread().interrupt();
						ex.printStackTrace();
						return false;
					}
				});
				localTime += System.nanoTime()-now;

				if (localCounter%1000==0){
					processCounter.add(localCounter);
					processTimeNanos.add(localTime);
					localTime = 0l;
					localCounter = 0l;
				}
			
		        // spin / sleep controllato
		       // LockSupport.parkNanos(config.NEURON_PERIOD_NANOS);
		    }
		});
		neuronThread.start();

		// IO loop for sensors/actuators/classifiers/supervisors
		logger.info("sensors/actuators/classifiers/supervisors thread started!");
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
							logger.info("Classifier result:{}", result);
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
		ioTask = scheduler.scheduleAtFixedRate(
				ioRunnable,
				0,
				config.IO_PERIOD_NANOS,
				TimeUnit.NANOSECONDS
				);

		if (metricsRecorder!=null) {
			LongAdder runs = new LongAdder();
			metricsRecorderTask = scheduler.scheduleAtFixedRate(
				() -> {
					long now = now();					
					long count = processCounter.sumThenReset();
					long totalNanos = processTimeNanos.sumThenReset();
					runs.add( count );
					long avgTimeProcessing = (count==0)?0:TimeUnit.NANOSECONDS.toMicros(totalNanos / count);
					
					for (Layer layer : brain.getAllLayers()) {
						int layerId = layer.getLayerId();
						metricsRecorder.pollLayerStats(
							now, 
							avgTimeProcessing, 
							runs.sum(), 
							layerId
						);
					}
				},
				1_000,
				config.METRICS_PERIOD_NANOS, 
				TimeUnit.NANOSECONDS
				);
		}
	}

	public synchronized void stop() {
		logger.info("Engine stopped!");
		
		//if (neuronTask != null) neuronTask.cancel(true);
		if (ioTask != null) ioTask.cancel(true);
		if (metricsRecorderTask != null) metricsRecorderTask.cancel(true);
		if (neuronThread!=null) neuronThread.interrupt();
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
		logger.info("Sensor attached:{}",s );
		this.sensors.add(Objects.requireNonNull(s));
	}

	public void attachActuator(IActuator a) {
		logger.info("Actuator attached:{}",a );
		this.actuators.add(Objects.requireNonNull(a));
	}

	public void attachClassifier(IClassifier<?> c) {
		logger.info("Classifier attached:{}",c );
		this.classifiers.add(Objects.requireNonNull(c));
	}

	public void attachSupervisor(ISupervisor<?> s) {
		logger.info("Supervisor attached:{}", s );
		this.supervisors.add(Objects.requireNonNull(s));
	}
}
