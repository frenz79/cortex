package com.cortex.base.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CompactBuffer<T> {
	
	  private final ByteBuffer buffer;
	    private final int elementSize;
	    private final Accessor<T> accessor;
	    private final int capacity;

	    public interface Accessor<T> {
	        int size();
	        void write(ByteBuffer buf, int index, T value);
	        void read(ByteBuffer buf, int index, T target);
	    }

	    public CompactBuffer(int capacity, Accessor<T> accessor) {
	        this.capacity = capacity;
	        this.accessor = accessor;
	        this.elementSize = accessor.size();
	        this.buffer = ByteBuffer.allocateDirect(capacity * elementSize)
	                                .order(ByteOrder.nativeOrder());
	    }

	    public void set(int index, T value) {
	        accessor.write(buffer, index * elementSize, value);
	    }

	    public void get(int index, T target) {
	        accessor.read(buffer, index * elementSize, target);
	    }

	    public int capacity() {
	        return capacity;
	    }

}
