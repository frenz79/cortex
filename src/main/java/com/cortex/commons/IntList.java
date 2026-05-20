package com.cortex.commons;

import java.util.Arrays;

public final class IntList {
	private int[] data;
	private int size;

	public IntList() {
		this(8);
	}

	public IntList(int cap) {
		data = new int[cap];
		size = 0;
	}

	public void add(int v) {
		if (size == data.length)
			data = Arrays.copyOf(data, data.length * 2);
		data[size++] = v;
	}

	public int get(int i) {
		return data[i];
	}

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}

	public int[] toArray() {
		return Arrays.copyOf(data, size);
	}

	public boolean isEmpty() {
		return size==0;
	}

}
