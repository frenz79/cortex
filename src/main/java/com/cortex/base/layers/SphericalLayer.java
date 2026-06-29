package com.cortex.base.layers;

import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;

public final class SphericalLayer extends Abstract3DLayer {

	private final float gr;
	private final float lambda;
	private final int counter;
	private final float radius;

	public SphericalLayer(LayerConfig config) {
		super(config);
		gr = (float) (3-Maths.sqrt(5));
		lambda = (float) (Maths.PI * gr);
		counter = getNeuronsCount();
		radius = config.DIMENSION;
	}

	@Override
	protected Point3f getPosition( int i ) {
		//  golden spiral / Fibonacci sphere variation
		float t = (float)i/counter;
		float a1 = (float) Maths.acos(1-2*t);
		float a2 = lambda * i;
		float sina1 = (float)Maths.sin(a1)* radius;

		float x = sina1 * (float)Maths.cos(a2);
		float y = sina1 * (float)Maths.sin(a2);
		float z = (float) Maths.cos(a1) * radius;

		return new Point3f(x,y,z);
	}
}
