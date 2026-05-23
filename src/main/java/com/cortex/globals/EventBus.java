package com.cortex.globals;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {

	public static record SynapseUpdatedData (
			float oldValue,
			float newValue
			) {}

	public static enum EventType {
		NEURON_FIRED,
		SYNAPSE_UPDATED
	}

	public static final EnumMap<EventType, List<EventListener>> listeners = new EnumMap<>(EventType.class);

	private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	static {
		for (EventType type : EventType.values()) {
			listeners.put(type, new CopyOnWriteArrayList<>());
		}
	}

	public static void fire( EventType type, long time, Object source, Object data ) {
		var list = listeners.get(type);
		if (list == null || list.isEmpty()) return;

		executor.submit(() -> {
			for (EventListener l : list) {
				try {
					l.onEvent(type, time, source, data);
				} catch (Exception ex) {
					System.err.println("EventBus listener error: " + ex.getMessage());
				}
			}
		});
	}

	public static void addListener( EventType type, EventListener l ) {
		listeners.compute(type, (k,v) -> {
			if (v==null) {
				v = new ArrayList<>();
			}
			v.add(l);
			return v;
		} );
	}	

	public static interface EventListener {
		public void onEvent( EventType type, long time, Object source, Object data );
	}
}
