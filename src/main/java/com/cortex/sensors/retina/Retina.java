package com.cortex.sensors.retina;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.commons.modules.ISensor;

public class Retina implements ISensor {

	private static final String SENSOR_ID = "RETINA";
	
	private final RetinaNeuron[][] retinaNeurons;
	private final int retinaW;
	private final int retinaH;

	private float[][] sourceLuminance;
	private int sourceWidth;
	private int sourceHeight;
	private float sourceScaleX;
	private float sourceScaleY;
	
    private float microDx = 0;
    private float microDy = 0;
    private boolean active;
    private long lastSaccadeTime = 0;
    private long lastProcessTime = 0;

    private static final long SAMPLING_PERIOD = 20_000_000; // 20 ms
    private static final long MICROSACCADE_PERIOD = 40_000_000; // 40 ms
    private static final float MICROSACCADE_AMPLITUDE = 0.5f;
    private static final int RECEPTIVE_RADIUS = 1;
    
    public Retina( int retinaW, int retinaH ) {
    	this.active = false;
    	this.retinaW = retinaW;
    	this.retinaH = retinaH;
    	this.retinaNeurons = new RetinaNeuron[retinaW][retinaH];
    	for (int x = 0; x < retinaW; x++) {
    	    for (int y = 0; y < retinaH; y++) {
    	        retinaNeurons[x][y] = new RetinaNeuron();
    	    }
    	}
    }
    
    @Override
   	public boolean process(long currTimeNanos) throws InterruptedException {
    	this.lastProcessTime = currTimeNanos;
    	if (!active || sourceLuminance==null) {
    		return true;
    	}
    	
    	updateMicrosaccades( currTimeNanos );
    	
    	for (int x = 0; x < retinaW; x++) {
    	    for (int y = 0; y < retinaH; y++) {
        		float lum = sampleLuminanceFromSource(x, y);
        		if ( retinaNeurons[x][y].process(currTimeNanos, lum)>0 ) {
        			List<Spike> spikes = retinaNeurons[x][y].drainSpikes();
        			retinaNeurons[x][y].fire( spikes );
        		}
        	}
        }
    	return true;
   	}
    
    private boolean updateMicrosaccades(long timeNanos) {
        if (timeNanos - lastSaccadeTime > (MICROSACCADE_PERIOD + ThreadLocalRandom.current().nextInt(1_000_000, 5_000_000))) {
            microDx = randomGaussian() * MICROSACCADE_AMPLITUDE;
            microDy = randomGaussian() * MICROSACCADE_AMPLITUDE;
            lastSaccadeTime = timeNanos;
            return true;
        }
        return false;
    }

    private float sampleLuminanceFromSource(int rx, int ry) {
        int cx = (int)((rx + 0.5f + microDx) * sourceScaleX);
        int cy = (int)((ry + 0.5f + microDy) * sourceScaleY);

        int radius = RECEPTIVE_RADIUS;
        float sum = 0.0f;
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int sx = clamp(cx + dx, 0, sourceWidth - 1);
                int sy = clamp(cy + dy, 0, sourceHeight - 1);
                sum += sourceLuminance[sx][sy];
                count++;
            }
        }
        // Luminance clamp
        return Math.max(0f, Math.min(1f, sum / count));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float randomGaussian() {
        return (float)ThreadLocalRandom.current().nextGaussian();
    }

	@Override
	public long getWaitTime() {
		return SAMPLING_PERIOD;
	}

	@Override
	public long getLastProcessTime() {
		return this.lastProcessTime;
	}

	public void setImage(BufferedImage image) {
		this.sourceWidth = image.getWidth();
		this.sourceHeight = image.getHeight();
		this.sourceScaleX = (float)sourceWidth / (float)retinaW;
		this.sourceScaleY = (float)sourceHeight / (float)retinaH;
		this.sourceLuminance = new float[image.getWidth()][image.getHeight()];
		
		for (int x=0; x<sourceLuminance.length; x++) {
        	for (int y=0; y<sourceLuminance[x].length; y++) {
        		sourceLuminance[x][y] = calculateLuminance(x,y, image) + randomGaussian() * 0.002f;
        	}
        }
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
	public boolean isActive() {
		return active;
	}

	@Override
	public void stop() {
		this.active = false;
	}

	@Override
	public void start() {
		this.active = true;
	}

	@Override
	public AbstractNeuron[][] getNeurons() {
		return retinaNeurons;
	}
}
