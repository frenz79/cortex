package com.cortex.base.utils;

public record Point3f (float x, float y, float z) {

	public float distance(Point3f pos) {
		return Maths.distance(this, pos);
	}

}
