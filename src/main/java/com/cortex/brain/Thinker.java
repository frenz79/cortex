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
import com.cortex.layer.Layer;
import com.cortex.layer.LayerConfig;
import com.cortex.layer.MultiLayer;
import com.cortex.layer.MultiLayerConfig;
import com.cortex.sensors.Sensor;

public class Thinker<MC extends MultiLayerConfig<C>, C extends LayerConfig, L extends Layer<C>> {

	private final MultiLayer<MC,C,L> layer;
	private final List<Sensor> sensors;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public Thinker(MultiLayer<MC,C,L> layer) {
		super();
		this.layer = layer;
		this.sensors = new ArrayList<>();
	}
	
	public void start() {
		AtomicLong clock = new AtomicLong(System.nanoTime());
		
		// Neurons loop
		new Thread(() -> {
			try {
				long statsTimer = 0;
				Function<AbstractNeuron, Boolean> activeNeuronsConsumer = n -> {
					try {
						return n.process(statsTimer);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return true;
				};
				
				while (true) {
					// Update clock
					long now = clock.get();				
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
						if (s.isActive()) {
							if ((now-s.getLastProcessTime())>s.getWaitTime()) {
								s.process(now);
							}
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
				System.out.println("STATS ActiveNeuronsCount:"+AbstractNeuron.getNeuronsCount());
			}			
		}, 3, 3, TimeUnit.SECONDS);
	}
	
	public void attachSensor( Sensor sensor ) {
		this.sensors.add( sensor );
	}
}
