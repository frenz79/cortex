package com.cortex.sensors.retina;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Spike;
import com.cortex.base.config.LayerConfig;
import com.cortex.commons.modules.ISensor;

public class Retina implements ISensor {

	private static final String SENSOR_ID = "RETINA";
	
	private final RetinaConfig retinaConfig;
	private final RetinaNeuron[][] retinaNeurons;
	private volatile float[][] sourceLuminance;
	private int sourceWidth;
	private int sourceHeight;
	private float sourceScaleX;
	private float sourceScaleY;
	
    private float microDx = 0;
    private float microDy = 0;
    private volatile boolean active;
    private volatile long lastSaccadeTime = System.nanoTime();
    private volatile long lastProcessTime = 0;
    
    public Retina( RetinaConfig retinaConfig, RetinaNeuronConfig neuronsConfig ) {
    	this.active = false;
    	this.retinaConfig = retinaConfig;
    	this.retinaNeurons = new RetinaNeuron[retinaConfig.RETINA_W][retinaConfig.RETINA_H];
    	int counter = 0;
    	for (int x = 0; x < retinaConfig.RETINA_W; x++) {
    	    for (int y = 0; y < retinaConfig.RETINA_H; y++) {
    	        retinaNeurons[x][y] = new RetinaNeuron(counter++, neuronsConfig );
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
    	
    //	int totalSpikes = 0;
    	
    	for (int x = 0; x < retinaConfig.RETINA_W; x++) {
    	    for (int y = 0; y < retinaConfig.RETINA_H; y++) {
        		float lum = sampleLuminanceFromSource(x, y);
        		 int c = retinaNeurons[x][y].process(currTimeNanos, lum);
        	        if (c > 0) {
     //   	            totalSpikes += c;
        	            List<Spike> spikes = retinaNeurons[x][y].drainSpikes();
        	            for (Spike s : spikes) {
        	                retinaNeurons[x][y].fire(s);
        	            }
        	        }
        	}
        }
    //	if (totalSpikes > 0) {
    //	    System.out.println("Retina spikes this frame: " + totalSpikes);
    //	}
    	return true;
   	}
    
    private boolean updateMicrosaccades(long timeNanos) {
        if (timeNanos - lastSaccadeTime > (retinaConfig.MICROSACCADE_PERIOD_NANOS + ThreadLocalRandom.current().nextInt(1_000_000, 5_000_000))) {
            microDx = ISensor.randomGaussian() * retinaConfig.MICROSACCADE_AMPLITUDE;
            microDy = ISensor.randomGaussian() * retinaConfig.MICROSACCADE_AMPLITUDE;
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

	@Override
	public long getWaitTimeNanos() {
		return retinaConfig.SAMPLING_PERIOD_NANOS;
	}

	@Override
	public long getLastProcessTime() {
		return this.lastProcessTime;
	}

	public void setImage(BufferedImage image) {
		this.sourceWidth = image.getWidth();
		this.sourceHeight = image.getHeight();
		this.sourceScaleX = (float)sourceWidth / (float)retinaConfig.RETINA_W;
		this.sourceScaleY = (float)sourceHeight / (float)retinaConfig.RETINA_H;
		this.sourceLuminance = new float[image.getWidth()][image.getHeight()];
		
		for (int x=0; x<sourceLuminance.length; x++) {
        	for (int y=0; y<sourceLuminance[x].length; y++) {
        		sourceLuminance[x][y] = calculateLuminance(x,y, image) + ISensor.randomGaussian() * 0.002f;
        	}
        }
		
		System.out.println("Image loaded into retina");
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
