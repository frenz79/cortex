package com.cortex.commons;

import javax.vecmath.Point3f;

import net.jafama.FastMath;

public class Maths {

	public static final float distance(Point3f p1, Point3f p2) {
		// p1.distance(p2);
		return (float) FastMath.sqrtQuick( 
				  FastMath.pow2(p1.x - p2.x) 
			    + FastMath.pow2(p1.y - p2.y) 
			    + FastMath.pow2(p1.z - p2.z));
	}
	
	public static final float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
