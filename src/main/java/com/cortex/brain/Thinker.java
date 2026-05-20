package com.cortex.brain;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.cortex.base.Neuron;
import com.cortex.brain.GlobalContext.LayerStats;
import com.cortex.commons.modules.IActuator;
import com.cortex.commons.modules.IClassifier;
import com.cortex.commons.modules.ISensor;
import com.cortex.commons.modules.ISupervisor;
import com.cortex.layer.Layer;
import com.cortex.layer.LayerConfig;
import com.cortex.layer.MultiLayer;
import com.cortex.layer.MultiLayerConfig;

public class Thinker<MC extends MultiLayerConfig<C>, C extends LayerConfig, L extends Layer<C>> {

	private final MultiLayer<MC,C,L> layer;
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
       
    public Thinker(MultiLayer<MC,C,L> layer) {
        this.layer = Objects.requireNonNull(layer);
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
        	GlobalContext.tick(now);
        	
            try {
                // Stream active neurons; pass the current time to each process call
                final long timestamp = now;
                final Function<Neuron, Boolean> activeNeuronsConsumer = n -> {
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
                GlobalContext.streamActiveNeuron(activeNeuronsConsumer);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };

        // IO loop for sensors/actuators/classifiers/supervisors
        Runnable ioRunnable = () -> {
            long now = GlobalContext.now();
            try {
                for (ISensor s : sensors) {
                    if (s.isActive() && (now - s.getLastProcessTime()) > s.getWaitTime()) {
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
                        Neuron result = c.classify(now);
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

        // scheduled reporter already present; keep using scheduler
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("== Avg Process Time:" + GlobalContext.getAverageProcessTimeMillis() + "ms ===========");
                for (L l : layer.getAllLayers()) {
                    LayerStats stats = GlobalContext.getAndResetStats(l.getId());
                    if (stats != null) {
                        System.out.println("Layer:" + l.getId() + " | ACT:" + stats.activeNeurons()
                            + " | SYN_W:" + stats.averageSynapticWeight() + " | SPIKES:" + stats.spikesCount());
                    } else {
                        System.out.println("Layer:" + l.getId() + " NO STATS");
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
