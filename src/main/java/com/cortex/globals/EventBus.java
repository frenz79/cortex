package com.cortex.globals;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventBus {

	protected static final Logger logger = LogManager.getLogger(EventBus.class);
		
	public static record SynapseUpdatedData (
			float oldValue,
			float newValue
			) {}

	public static record SynapseSpikedData (
			boolean isPre
			) {
		
		private static final SynapseSpikedData PRE_SPIKE = new SynapseSpikedData(true);
		private static final SynapseSpikedData POST_SPIKE = new SynapseSpikedData(false);
		
		public static SynapseSpikedData preSpikeData() {
			return PRE_SPIKE;
		}
		
		public static SynapseSpikedData postSpikeData() {
			return POST_SPIKE;
		}
	}
	
	public static enum EventType {
		NEURON_FIRED,
		NEURON_READY_TO_FIRE,
		SYNAPSE_UPDATED,
		LAYER_STATS,
		SYNAPSE_SPIKED
	}

	public static final EnumMap<EventType, List<EventListener>> listeners = new EnumMap<>(EventType.class);

	private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	static {
		for (EventType type : EventType.values()) {
			listeners.put(type, new CopyOnWriteArrayList<>());
		}
	}

	public static void fire( EventType type, long time, int sourceIdx, Object data ) {
		try {
			var list = listeners.get(type);
			if (list == null || list.isEmpty()) return;
			if (list.size()==1) {
				list.get(0).onEvent(type, time, sourceIdx, data);
			} else {
				for (EventListener l : list) {
					try {
						l.onEvent(type, time, sourceIdx, data);
					} catch (Exception ex) {
						logger.error("EventBus listener error:", ex);
					}
				}
			}
			/*
			executor.submit(() -> {
				for (EventListener l : list) {
					try {
						l.onEvent(type, time, source, data);
					} catch (Exception ex) {
						logger.error("EventBus listener error:", ex);
					}
				}
			});
			*/
		} catch (Exception ex) {
			logger.error("EventBus listener error:", ex);
		}
	}

	public static void addListener( EventType type, EventListener l ) {
		listeners.get(type).add(l);
	}	

	public static interface EventListener {
		public void onEvent( EventType type, long time, int sourceIdx, Object data );
	}
}
