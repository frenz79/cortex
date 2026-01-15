package com.cortex.layer;

public class MultiSphericalLayer extends MultiLayer<MultiSphericalLayerConfig, SphericalLayerConfig, SphericalLayer>{

	public MultiSphericalLayer(MultiSphericalLayerConfig config) {
		super(config);
		if ( config.getNumberOfLayers()<3 ) {
			throw new RuntimeException("At least 3 layers exptected");
		}
	}

	@Override
	public SphericalLayer buildLayer( SphericalLayerConfig cfg ) {
		SphericalLayer l = new SphericalLayer(cfg);
		l.generateNeurons();
		l.connectInternal();
		return l;
	}
	
	@Override
	public int getNeuronsCount() {
		int ret = 0;
		for ( SphericalLayer l : layers ) {
			ret += l.getNeuronsCount();
		}
		return ret;
	}

	@Override
	public int getSynapsesCount() {
		int ret = 0;
		for ( SphericalLayer l : layers ) {
			ret += l.getSynapsesCount();
		}
		return ret;
	}	
}
