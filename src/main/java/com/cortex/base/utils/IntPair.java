package com.cortex.base.utils;

import java.util.Objects;

public record IntPair (int left, int right) {

	@Override
	public int hashCode() {
		return Objects.hash(left, right);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntPair other = (IntPair) obj;
		return left == other.left && right == other.right;
	}

	@Override
	public String toString() {
		return "IntPair [left=" + left + ", right=" + right + "]";
	}
}
