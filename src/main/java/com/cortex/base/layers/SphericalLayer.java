package com.cortex.base.layers;

import com.cortex.base.utils.Maths;
import com.cortex.base.utils.Point3f;
import com.cortex.brain.Emisphere.CorticalNeuronFactory;

public final class SphericalLayer extends Abstract3DLayer {
	
	public SphericalLayer(LayerConfig config) {
		super(config);
	}

	@Override
	protected SphericalLayer internalPopulate( CorticalNeuronFactory neuronFactory ) {
		//  golden spiral / Fibonacci sphere variation
		float gr = (float) (3-Maths.sqrt(5));
		float lambda = (float) (Maths.PI * gr);
		final int counter = getNeuronsCount();
		final float radius = config.DIMENSION;

		for(int i=0; i<counter; i++){
			float t = (float)i/counter;
			float a1 = (float) Maths.acos(1-2*t);
			float a2 = lambda * i;
			float sina1 = (float)Maths.sin(a1)* radius;

			float x = sina1 * (float)Maths.cos(a2);
			float y = sina1 * (float)Maths.sin(a2);
			float z = (float) Maths.cos(a1) * radius;

			this.neurons[i] = neuronFactory.buildNeuron(
				config.getLayerId(),
				isInhibitor(), new Point3f(x,y,z)
			);
		}
		return this;
	}
}
