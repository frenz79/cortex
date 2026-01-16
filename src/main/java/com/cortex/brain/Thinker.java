package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import com.cortex.actuators.Actuator;
import com.cortex.base.AbstractNeuron;
import com.cortex.base.Monitor;
import com.cortex.base.Monitor.LayerStats;
import com.cortex.classifiers.Classifier;
import com.cortex.layer.Layer;
import com.cortex.layer.LayerConfig;
import com.cortex.layer.MultiLayer;
import com.cortex.layer.MultiLayerConfig;
import com.cortex.sensors.Sensor;

public class Thinker<MC extends MultiLayerConfig<C>, C extends LayerConfig, L extends Layer<C>> {

	private final MultiLayer<MC,C,L> layer;
	private final List<Sensor> sensors;
	private final List<Actuator> actuators;
	private final List<Classifier<?>> classifiers;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public Thinker(MultiLayer<MC,C,L> layer) {
		super();
		this.layer = layer;
		this.sensors = new ArrayList<>();
		this.actuators = new ArrayList<>();
		this.classifiers = new ArrayList<>();
	}
	
	public void start() {
		AtomicLong clock = new AtomicLong(System.nanoTime());
		
		// Neurons loop
		new Thread(() -> {
			try {
				Function<AbstractNeuron, Boolean> activeNeuronsConsumer = n -> {
					try {
						return n.process(clock.get());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return true;
				};
				
				while (true) {		
					clock.set(System.nanoTime());
					AbstractNeuron.forEachActive( activeNeuronsConsumer );
					LockSupport.parkNanos(50_000); // 50 µs			
				}
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		}).start();
		
		// Sensors and actuators		
		new Thread(() -> {
			try {
				while (true) {
					long now = clock.get();
					for (Sensor s : sensors ) {
						if (s.isActive() && (now-s.getLastProcessTime())>s.getWaitTime()) {
							s.process(now);
						}
					}
					
					for (Actuator a : actuators ) {
						if (a.isActive() && (now-a.getLastProcessTime())>a.getWaitTime()) {
							a.process(now);
						}
					}
					for (Classifier<?> c : classifiers ) {
						AbstractNeuron result = c.classify(now);
						if (result!=null) {
							System.out.println("Classifier result:"+result.toString());
						}
					}					
					
					LockSupport.parkNanos(50_000); // 50 µs
				}
				
			} catch ( Exception ex ) {
				ex.printStackTrace();
			}
		}).start();
		
		scheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					for ( L l : layer.getAllLayers() ) {
						LayerStats stats = Monitor.getAndResetStats(l.getId());
						if(stats!=null) {
							System.out.println("Layer:"+l.getId()+" | ACT:"+stats.activeNeurons()+" | SYN_W:"+stats.averageSynapticWeight()+" | SPIKES:"+stats.spikesCount());
						} else {
							System.out.println("Layer:"+l.getId()+" NO STATS");
						}
					}
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}			
		}, 3, 3, TimeUnit.SECONDS);
	}
	
	public void attachSensor( Sensor s ) {
		this.sensors.add( s );
	}
	
	public void attachActuator( Actuator a ) {
		this.actuators.add( a );
	}
	
	public void attachClassifier( Classifier<?> c ) {
		this.classifiers.add( c );
	}
}
