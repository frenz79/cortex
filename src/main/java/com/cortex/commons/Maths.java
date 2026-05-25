package com.cortex.commons;

import javax.vecmath.Point3f;

import net.jafama.FastMath;

public class Maths {

	public static final double PI = FastMath.PI;

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

	public static final int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
	
	public static final float abs(float v) {
		return FastMath.abs(v);
	}
	
	public static final float zeroIfSmall( float v ) {
		return (FastMath.abs(v) < 1e-6f)?0f:v;
	}

	public static final double pow(long v, long e) {
		return FastMath.pow(v, e);
	}

	public static final double exp(double d) {
		return FastMath.exp(d);
	}

	public static final double pow(float v, double e) {
		return FastMath.pow(v, e);
	}

	public static final float max(float f, float g) {
		return FastMath.max(f, g);
	}
	
	public static final long max(long f, long g) {
		return FastMath.max(f, g);
	}
	
	public static final int min(int f, int g) {
		return FastMath.min(f, g);
	}
	
	public static final float min(float f, float g) {
		return FastMath.min(f, g);
	}

	public static final float signum(float f) {
		return FastMath.signum(f);
	}

	public static final double sqrt(float f) {
		return FastMath.sqrtQuick(f);
	}

	public static final int floor(double d) {
		return FastMath.floorToInt(d);
	}

	public static final double cos(float d) {
		return FastMath.cos(d);
	}

	public static final double sin(float d) {
		return FastMath.sin(d);
	}

	public static final double asin(float f) {
		return FastMath.asin(f);
	}
	
	public static final double acos(float f) {
		return FastMath.acos(f);
	}

	public static final double max(double a, double b) {
		return FastMath.max(a, b);
	}

	public static final double abs(double d) {
		return FastMath.abs(d);
	}

	public static final double sqrt(double d) {
		return FastMath.sqrt(d);
	}
}
