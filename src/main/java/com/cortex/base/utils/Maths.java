package com.cortex.base.utils;

import java.util.concurrent.ThreadLocalRandom;

import net.jafama.FastMath;

public class Maths {

	public static final double PI = FastMath.PI;

	private static final int GAUSSIAN_TABLE_SIZE = 4096;
	public static final float[] GAUSSIAN_TABLE = new float[GAUSSIAN_TABLE_SIZE];

	static {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		for (int i = 0; i < GAUSSIAN_TABLE_SIZE; i++) {
			// Box–Muller real gaussian
			float u1 = rnd.nextFloat();
			float u2 = rnd.nextFloat();
			float r = (float)Maths.sqrt(-2.0f * Math.log(u1));
			float theta = (float)(2.0 * PI * u2);
			GAUSSIAN_TABLE[i] = r * (float)Maths.cos(theta);
		}
	}

	public static final float nextGaussian() {
		return GAUSSIAN_TABLE[ThreadLocalRandom.current().nextInt(GAUSSIAN_TABLE_SIZE)];
	}

	public static final float distance(Point3f p1, Point3f p2) {
		// p1.distance(p2);
		return (float) FastMath.sqrtQuick( 
				FastMath.pow2(p1.x() - p2.x()) 
				+ FastMath.pow2(p1.y() - p2.y()) 
				+ FastMath.pow2(p1.z() - p2.z()));
	}

	public static final float clamp(float v, float min, float max) {
	//	return Math.max(min, Math.min(max, v));
		if (v < min) return min;
		if (v > max) return max;
		return v;
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
		return FastMath.powQuick(v, e);
	}
/*
	public static final double exp(double d) {
		return FastMath.expQuick(d);
	}
*/
	public static float exp(float x) {
	    x = 1.0f + x / 256.0f;
	    x *= x; x *= x; x *= x; x *= x;
	    x *= x; x *= x; x *= x; x *= x;
	    return x;
	}
	
	public static final double pow(float v, double e) {
		return FastMath.powQuick(v, e);
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
		return FastMath.cosQuick(d);
	}

	public static final double sin(float d) {
		return FastMath.sinQuick(d);
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
