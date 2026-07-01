package com.cortex.externals;

public interface IExternalLogic {

	void process(long now);

	long getWaitTimeNanos();

	String getExternalModuleId();

}
