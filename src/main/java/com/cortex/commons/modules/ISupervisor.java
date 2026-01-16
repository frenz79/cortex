package com.cortex.commons.modules;

public interface ISupervisor<T> {

	public void process(long now);
	public void setExpected(T expected);
}
