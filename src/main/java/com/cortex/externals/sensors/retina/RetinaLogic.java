package com.cortex.externals.sensors.retina;

import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.soa.constants.DirectionCode;
import com.cortex.base.soa.logic.SpikeRingBufferLogic;
import com.cortex.base.utils.Maths;
import com.cortex.externals.AbstractExtModuleLogic;

public final class RetinaLogic extends AbstractExtModuleLogic {

	private static final String SENSOR_ID = "RETINA";
	
	private final RetinaSoA retinaSoA;
	private final RetinaConfig retinaConfig;

	// sorgente luminanza
	private float[][] sourceLuminance;
	private float[][] integralImage;
	private float sourceScaleX;
	private float sourceScaleY;
	private int sourceWidth;
	private int sourceHeight;

	// microsaccade
	private float microDx = 0f;
	private float microDy = 0f;
	private long lastSaccadeTime = 0;

	public RetinaLogic(
		RetinaSoA retinaSoA,
		SpikeRingBufferLogic spikeBufferLogic,
		RetinaConfig retinaConfig
	) {
		super(SENSOR_ID, retinaConfig.SAMPLING_PERIOD_NANOS, retinaConfig.TARGET_HEMISPHERE_ID, retinaConfig.TARGET_LAYER_ID, spikeBufferLogic);
		this.retinaSoA = retinaSoA;
		this.retinaConfig = retinaConfig;
	}

	private int getSpikesCount( float raw, int idx) {		
		int spikeCount = (int)raw;
		float fractional = raw - spikeCount;
		if (ThreadLocalRandom.current().nextFloat() < fractional)
		    spikeCount++;
		// Clamp
		return Maths.min(spikeCount, retinaSoA.maxSpikesPerSample[idx]);
	}
	
	@Override
	public void processInternal(long now) {
		if (sourceLuminance == null)
			return;

		updateMicrosaccades(now);

		int W = retinaConfig.RETINA_W;
		int H = retinaConfig.RETINA_H;

		int idx = 0; // indice retinaSoA

		for (int x = 0; x < W; x++) {
			for (int y = 0; y < H; y++, idx++) {

				float lum = sampleLuminanceFromIntegral(x, y);

				float last = retinaSoA.lastLuminance[idx];
				float delta = lum - last;
				retinaSoA.lastLuminance[idx] = lum;

				float absDelta = Math.abs(delta);
				if (absDelta < retinaSoA.threshold[idx])
					continue;

				float gain = delta > 0 ? retinaSoA.onGain[idx] : retinaSoA.offGain[idx];
				float raw = absDelta * gain;
				int spikeCount = getSpikesCount(raw, idx);
				
				if (spikeCount == 0)
					continue;

				fire(now, idx, raw, delta < 0);
			}
		}
	}

	private void updateMicrosaccades(long now) {
		if (now - lastSaccadeTime >
		retinaConfig.MICROSACCADE_PERIOD_NANOS +
		ThreadLocalRandom.current().nextInt(1_000_000, 5_000_000)) {
			microDx = (float)Maths.nextGaussian() * retinaConfig.MICROSACCADE_AMPLITUDE;
			microDy = (float)Maths.nextGaussian() * retinaConfig.MICROSACCADE_AMPLITUDE;
			lastSaccadeTime = now;
		}
	}

	private float sampleLuminanceFromIntegral(int rx, int ry) {
		int cx = (int)((rx + 0.5f + microDx) * sourceScaleX);
		int cy = (int)((ry + 0.5f + microDy) * sourceScaleY);

		int r = retinaConfig.RECEPTIVE_RADIUS;

		int x1 = Maths.clamp(cx - r, 0, sourceWidth - 1);
		int y1 = Maths.clamp(cy - r, 0, sourceHeight - 1);
		int x2 = Maths.clamp(cx + r, 0, sourceWidth - 1);
		int y2 = Maths.clamp(cy + r, 0, sourceHeight - 1);

		float sum = sumRegion(integralImage, x1, y1, x2, y2);
		int area = (x2 - x1 + 1) * (y2 - y1 + 1);

		return Maths.clamp(sum / area, 0f, 1f);
	}

	private static float sumRegion(float[][] integral, int x1, int y1, int x2, int y2) {
		float A = (x1 > 0 && y1 > 0) ? integral[x1 - 1][y1 - 1] : 0;
		float B = (y1 > 0) ? integral[x2][y1 - 1] : 0;
		float C = (x1 > 0) ? integral[x1 - 1][y2] : 0;
		float D = integral[x2][y2];
		return D - B - C + A;
	}

	public void setImage(BufferedImage image) {
		this.sourceWidth = image.getWidth();
		this.sourceHeight = image.getHeight();
		this.sourceScaleX = (float)sourceWidth / retinaConfig.RETINA_W;
		this.sourceScaleY = (float)sourceHeight / retinaConfig.RETINA_H;

		this.sourceLuminance = new float[sourceWidth][sourceHeight];

		for (int x = 0; x < sourceWidth; x++) {
			for (int y = 0; y < sourceHeight; y++) {
				sourceLuminance[x][y] =
						calculateLuminance(x, y, image) +
						(float)Maths.nextGaussian() * 0.002f;
			}
		}

		this.integralImage = buildIntegral(sourceLuminance, sourceWidth, sourceHeight);
	}

	private static float[][] buildIntegral(float[][] src, int width, int height) {
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
	protected int getDirection() {
		return DirectionCode.OUTGOING;
	}
}

