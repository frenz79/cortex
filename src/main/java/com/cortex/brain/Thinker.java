package com.cortex.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Monitor;
import com.cortex.base.Monitor.LayerStats;
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
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public Thinker(MultiLayer<MC,C,L> layer) {
		super();
		this.layer = layer;
		this.sensors = new ArrayList<>();
		this.actuators = new ArrayList<>();
		this.classifiers = new ArrayList<>();
		this.supervisors = new ArrayList<>();
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
		
		// Sensors / actuators / classifiers / supervisors		
		new Thread(() -> {
			try {
				while (true) {
					long now = clock.get();
					for (ISensor s : sensors ) {
						if (s.isActive() && (now-s.getLastProcessTime())>s.getWaitTime()) {
							s.process(now);
						}
					}					
					for (IActuator a : actuators ) {
						if (a.isActive() && (now-a.getLastProcessTime())>a.getWaitTime()) {
							a.process(now);
						}
					}
					for (IClassifier<?> c : classifiers ) {
						AbstractNeuron result = c.classify(now);
						if (result!=null) {
							System.out.println("Classifier result:"+result.toString());
						}
					}					
					for (ISupervisor<?> s : supervisors ) {
						s.process(now);
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
	
	public void attachSensor( ISensor s ) {
		this.sensors.add( s );
	}
	
	public void attachActuator( IActuator a ) {
		this.actuators.add( a );
	}
	
	public void attachClassifier( IClassifier<?> c ) {
		this.classifiers.add( c );
	}
	
	public void attachSupervisor( ISupervisor<?> s ) {
		this.supervisors.add( s );
	}
}
