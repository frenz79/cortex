package com.cortex.globals;

import java.util.Objects;
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
	private final ScheduledExecutorService scheduler;

	private Thread neuronThread;
	private ScheduledFuture<?> ioTask;
	private ScheduledFuture<?> metricsRecorderTask;

	private final Brain brain;
	private MetricsRecorder metricsRecorder;

	public NeuralEngine(Brain brain, NeuralEngineConfig config ) {
		this.brain = Objects.requireNonNull(brain);
		this.config = Objects.requireNonNull(config);
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
		final long samplingInterval = 100;
		final LongAdder processCounterTotal = new LongAdder();
		final LongAdder processTimeTotalNanos = new LongAdder();
		final AtomicLong lastProcessCounter = new AtomicLong();
		final AtomicLong lastProcessTimeNanos = new AtomicLong();

		Thread neuronThread = new Thread(() -> {
			logger.info("neuronThread started!");
			long localCounter = 0l;
			long localTime = 0l;
			while (!Thread.currentThread().isInterrupted()) {
				long start = System.nanoTime();
				tick(start);
				localCounter++;

				brain.streamActiveNeuron(n -> {
					try {
					//	long begin = System.nanoTime();
						boolean active = n.process(start);
					//	long elapsed = System.nanoTime() - begin;
/*
						if (elapsed > 5_000_000) { // >1ms
						    logger.warn("Neuron {} took {} ms", n.getIndex(), elapsed / 1_000_000.0);
						    logger.info("Neuron {}: inSynapses={}, outSynapses={}",
						    		n.getIndex(),
						    	    n.getInSynapses().size(),
						    	    n.getOutSynapses().size()
						    	);
						}
						*/
						return active;
						
					} catch (Exception ex) {
						Thread.currentThread().interrupt();
						ex.printStackTrace();
						return false;
					}
				});
				long elapsed = System.nanoTime()-start;

				processCounterTotal.increment();
				processTimeTotalNanos.add(elapsed);

				// spin / sleep controllato
				//LockSupport.parkNanos(config.NEURON_PERIOD_NANOS);
			}
		});
		neuronThread.start();

		// IO loop for sensors/actuators/classifiers/supervisors
		logger.info("sensors/actuators/classifiers/supervisors thread started!");
		Runnable ioRunnable = () -> {
			long now = now();
			try {
				for (ISensor s : brain.getSensors()) {
					if (s.isActive() && (now - s.getLastProcessTime()) > s.getWaitTimeNanos()) {
						s.process(now);
					}
				}
				for (IActuator a : brain.getActuators()) {
					if (a.isActive() && (now - a.getLastProcessTime()) > a.getWaitTime()) {
						a.process(now);
					}
				}
				for (IClassifier<?> c : brain.getClassifiers()) {
					try {
						AbstractNeuron result = c.classify(now);
						if (result != null) {
							logger.info("Classifier result:{}", result);
						}
					} catch (Exception ex) {
						logger.error("Handled Exception:",ex);
					}
				}
				for (ISupervisor<?> s : brain.getSupervisors()) {
					try {
						s.process(now);
					} catch (Exception ex) {
						logger.error("Handled Exception:",ex);
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
			metricsRecorderTask = scheduler.scheduleWithFixedDelay(
					() -> {
						long now = now();
				        long totalRuns = processCounterTotal.sum();
				        long totalTime = processTimeTotalNanos.sum();
				        long prevRuns = lastProcessCounter.getAndSet(totalRuns);
				        long prevTime = lastProcessTimeNanos.getAndSet(totalTime);
				        long runsWindow = totalRuns - prevRuns;
				        long timeWindow = totalTime - prevTime;

				        long avgTimeMicros = (runsWindow == 0)
				                ? 0
				                : TimeUnit.NANOSECONDS.toMicros(timeWindow / runsWindow);
						
						for (Layer layer : brain.getAllLayers()) {
							int layerId = layer.getLayerId();
							metricsRecorder.pollLayerStats(
								now, 
								avgTimeMicros, 
								runsWindow, 
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
}
