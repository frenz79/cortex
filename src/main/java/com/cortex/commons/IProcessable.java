package com.cortex.commons;

public interface IProcessable {

	/**
	 * @param currTimeNanos
	 * @return false if neuron has no more spikes left to process
	 * @throws InterruptedException
	 */
	public boolean process(long currTimeNanos) throws InterruptedException;
}
