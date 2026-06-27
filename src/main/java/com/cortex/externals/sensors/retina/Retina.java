package com.cortex.externals.sensors.retina;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Commons;
import com.cortex.base.Spike;
import com.cortex.base.Synapse;
import com.cortex.base.SynapseBranch;
import com.cortex.base.externals.ExternalConnConfig;
import com.cortex.base.externals.ISensor;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.base.soa.NeuronSoA;
import com.cortex.base.utils.Maths;

public class Retina extends ISensor {

	private final Logger logger = LogManager.getLogger(this.getClass());

	private static final String SENSOR_ID = "RETINA";

	private final RetinaConfig retinaConfig;
	private final RetinaNeuron[][] retinaNeurons;
	private volatile float[][] sourceLuminance;
	private volatile float[][] integralImage;
	private int sourceWidth;
	private int sourceHeight;
	private float sourceScaleX;
	private float sourceScaleY;

	private float microDx = 0;
	private float microDy = 0;

	private volatile long lastSaccadeTime = System.nanoTime();

	private final NeuronSoA neuronsStates;
	
	public Retina( RetinaConfig retinaConfig, RetinaNeuronConfig neuronsConfig ) {
		this.retinaConfig = retinaConfig;
		this.retinaNeurons = new RetinaNeuron[retinaConfig.RETINA_W][retinaConfig.RETINA_H];
		int counter = 0;
		this.neuronsStates = new NeuronSoA(retinaConfig.RETINA_W*retinaConfig.RETINA_H);
		
		for (int x = 0; x < retinaConfig.RETINA_W; x++) {
			for (int y = 0; y < retinaConfig.RETINA_H; y++) {
				retinaNeurons[x][y] = new RetinaNeuron(this.neuronsStates, counter++, neuronsConfig );
			}
		}
	}

	@Override
	public boolean processExt(long now) throws InterruptedException {
		if (sourceLuminance==null) {
			return true;
		}

		updateMicrosaccades( now );
		for (int x = 0; x < retinaConfig.RETINA_W; x++) {
			for (int y = 0; y < retinaConfig.RETINA_H; y++) {
				float lum = sampleLuminanceFromIntegral(x, y); //sampleLuminanceFromSource(x, y);
				RetinaNeuron n = retinaNeurons[x][y];
				int c = n.process(now, lum);
				if (c > 0) {
					List<Float> spikesAmplitude = retinaNeurons[x][y].drainSpikes();
					for (Float amplitude : spikesAmplitude) {
						for (SynapseBranch synapseBranch : retinaNeurons[x][y].getOutSynapseBranches()) {
							for ( Synapse synapse : synapseBranch.synapses ) {
								// No real delay, "ideal source"
								synapse.addSpike(
									now, Spike.createWithJitter(amplitude, (amplitude<0), now)
								);
							}
						}
					}
				}
			}
		}
		return true;
	}

	private float sampleLuminanceFromIntegral(int rx, int ry) {

		int cx = (int)((rx + 0.5f + microDx) * sourceScaleX);
		int cy = (int)((ry + 0.5f + microDy) * sourceScaleY);

		int r = retinaConfig.RECEPTIVE_RADIUS;

		int x1 = Maths.clamp(cx - r, 0, sourceWidth - 1);
		int y1 = Maths.clamp(cy - r, 0, sourceHeight - 1);
		int x2 = Maths.clamp(cx + r, 0, sourceWidth - 1);
		int y2 = Maths.clamp(cy + r, 0, sourceHeight - 1);

		float sum = sumRegion(this.integralImage, x1, y1, x2, y2);

		int area = (x2 - x1 + 1) * (y2 - y1 + 1);

		return Maths.clamp(sum / area, 0f, 1f);
	}


	private boolean updateMicrosaccades(long timeNanos) {
		if (timeNanos - lastSaccadeTime > (retinaConfig.MICROSACCADE_PERIOD_NANOS + ThreadLocalRandom.current().nextInt(1_000_000, 5_000_000))) {
			microDx = randomGaussian() * retinaConfig.MICROSACCADE_AMPLITUDE;
			microDy = randomGaussian() * retinaConfig.MICROSACCADE_AMPLITUDE;
			lastSaccadeTime = timeNanos;
			return true;
		}
		return false;
	}

	private float sampleLuminanceFromSource(int rx, int ry) {
		int cx = (int)((rx + 0.5f + microDx) * sourceScaleX);
		int cy = (int)((ry + 0.5f + microDy) * sourceScaleY);

		int radius = retinaConfig.RECEPTIVE_RADIUS;
		float sum = 0.0f;
		int count = 0;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				int sx = Maths.clamp(cx + dx, 0, sourceWidth - 1);
				int sy = Maths.clamp(cy + dy, 0, sourceHeight - 1);
				sum += sourceLuminance[sx][sy];
				count++;
			}
		}
		// Luminance clamp
		return Maths.clamp(sum / count, 0f, 1f );
	}

	@Override
	public long getWaitTimeNanos() {
		return retinaConfig.SAMPLING_PERIOD_NANOS;
	}

	public void setImage(BufferedImage image) {
		this.sourceWidth = image.getWidth();
		this.sourceHeight = image.getHeight();
		this.sourceScaleX = (float)sourceWidth / (float)retinaConfig.RETINA_W;
		this.sourceScaleY = (float)sourceHeight / (float)retinaConfig.RETINA_H;
		this.sourceLuminance = new float[image.getWidth()][image.getHeight()];

		for (int x=0; x<sourceLuminance.length; x++) {
			for (int y=0; y<sourceLuminance[x].length; y++) {
				sourceLuminance[x][y] = calculateLuminance(x,y, image) + randomGaussian() * 0.002f;
			}
		}

		// costruisci integral image UNA volta
		this.integralImage = buildIntegral(sourceLuminance, sourceWidth, sourceHeight);

		logger.info("Image loaded into retina");
	}

	private float sumRegion(float[][] integral, int x1, int y1, int x2, int y2) {
		float A = (x1 > 0 && y1 > 0) ? integral[x1 - 1][y1 - 1] : 0;
		float B = (y1 > 0) ? integral[x2][y1 - 1] : 0;
		float C = (x1 > 0) ? integral[x1 - 1][y2] : 0;
		float D = integral[x2][y2];
		return D - B - C + A;
	}

	private float[][] buildIntegral(float[][] src, int width, int height) {
		float[][] integral = new float[width][height];

		for (int x = 0; x < width; x++) {
			float rowSum = 0f;

			for (int y = 0; y < height; y++) {
				rowSum += src[x][y];

				if (x == 0) {
					integral[x][y] = rowSum;
				} else {
					integral[x][y] = integral[x - 1][y] + rowSum;
				}
			}
		}
		return integral;
	}

	private static float calculateLuminance(int x, int y, BufferedImage image) {
		int color = image.getRGB(x, y);
		// extract each color component
		int red   = (color >>> 16) & 0xFF;
		int green = (color >>>  8) & 0xFF;
		int blue  = (color >>>  0) & 0xFF;
		// calc luminance in range 0.0 to 1.0; using SRGB luminance constants
		return (red * 0.2126f + green * 0.7152f + blue * 0.0722f) / 255.0f;
	}

	@Override
	public String getId() {
		return SENSOR_ID;
	}

	@Override
	public AbstractNeuron[][] getNeurons() {
		return retinaNeurons;
	}

	@Override
	public String toString() {
		return "Retina [retinaNeurons=" + retinaNeurons.length + ", sourceWidth=" + sourceWidth + ", sourceHeight=" + sourceHeight
				+ ", sourceScaleX=" + sourceScaleX + ", sourceScaleY=" + sourceScaleY + ", microDx=" + microDx
				+ ", microDy=" + microDy + "]";
	}

	@Override
	public ExternalConnConfig getExternalConnConfig() {
		return ExternalConnConfig.newBuilder()
			.withConnections(100)
			.withMaxDistance(20.0f)
			.withNeuronFilter(Commons.SKIP_INHIBITOR_CONNECT_PREDICATE)
			.withLinkedLayerId(0)
			.withPlasticity(
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.0015f, 0.0025f, 40_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.01f, 0.06f, 0.00f, 0.03f)// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibilityDecaySeconds(0.997f) 	// ELIGIBILITY_DECAY
						.withPlasticity(20f, 5f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.02f) 			// HOMEOSTATIC_RATE
						.build(),
					InhibitorySynapticPlasticityConfig.newBuilder()
						.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
						.withLearningRate(0.005f)				// LEARNING_RATE
						.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
						.build()
						)	
				)
			.build();				
	}

	@Override
	public boolean isProducer() {
		return true;
	}
}
