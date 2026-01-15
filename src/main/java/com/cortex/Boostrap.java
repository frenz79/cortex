package com.cortex;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.cortex.brain.Thinker;
import com.cortex.classifiers.ocr.OCRClassifier;
import com.cortex.layer.MultiLayerConfig.IntraLayersConnConfig;
import com.cortex.layer.MultiSphericalLayer;
import com.cortex.layer.MultiSphericalLayerConfig;
import com.cortex.layer.SphericalLayerConfig;
import com.cortex.sensors.retina.Retina;

public class Boostrap {
	
	public static void main(String[] args) throws IOException { 
		int totalNeurons = 100_000;
		int fanOut = 1000;
		int connScale = (fanOut>=1000)?100:(fanOut>=100)?10:1;

		// Create network layers and synapses
		MultiSphericalLayerConfig cfg = buildMultiSphericalLayerConfig( totalNeurons, connScale );
		MultiSphericalLayer layer = new MultiSphericalLayer( cfg );
		
		System.out.println("Number of Neurons:"+layer.getNeuronsCount());
		System.out.println("Number of Synapses:"+layer.getSynapsesCount());

		// Create sensors
		Retina retina = new Retina(70 , 70);
		retina.setImage( loadImage("src/main/resources/Letter-A.png"));
		
		// Connect retina to L1
		int retinaConn = layer.getLayers(0).connect(	retina.getNeurons(), 10, 40, 0.5f, true);
		System.out.println("Number of Retina Synapses:"+retinaConn);
		
		// OCR Classifier
		OCRClassifier ocr = new OCRClassifier();
		int ocrConn = layer.getLayers(3).connect(	ocr.getNeurons(), 10, 40, 0.5f, false);
		System.out.println("Number of OCR Synapses:"+ocrConn);
		
		// This is the main processing loop
		Thinker<?,?,?> thinker = new Thinker<>( layer );
		thinker.attachSensor( retina );
		
		// Open UI
		// new SimpleViewer( layer, true, false );
		
		// ..give the life!
		retina.start();
		thinker.start();
	}
	
	private static BufferedImage loadImage(String path) throws IOException {
		return ImageIO.read(new File(path));
	}

	private static MultiSphericalLayerConfig buildMultiSphericalLayerConfig( int totalNeurons, int connScale) {
		MultiSphericalLayerConfig cfg = new MultiSphericalLayerConfig();
		// neurons, inhibitorFreq, minConnections, maxConnections, maxConnDistance
		cfg.addLayerConfig( new SphericalLayerConfig((int)(totalNeurons*0.15), 0.15f,  5*connScale,  7*connScale, 0.60f, 1.00f) ); // L1
		cfg.addLayerConfig( new SphericalLayerConfig((int)(totalNeurons*0.20), 0.25f,  8*connScale, 12*connScale, 0.50f, 0.85f) ); // L2
		cfg.addLayerConfig( new SphericalLayerConfig((int)(totalNeurons*0.25), 0.15f, 10*connScale, 15*connScale, 0.40f, 0.65f) ); // L3
		cfg.addLayerConfig( new SphericalLayerConfig((int)(totalNeurons*0.20), 0.30f, 12*connScale, 18*connScale, 0.30f, 0.45f) ); // L4
		cfg.addLayerConfig( new SphericalLayerConfig((int)(totalNeurons*0.12), 0.10f,  7*connScale, 10*connScale, 0.20f, 0.30f) ); // L5
		cfg.addLayerConfig( new SphericalLayerConfig((int)(totalNeurons*0.08), 0.20f,  8*connScale, 12*connScale, 0.10f, 0.15f) ); // L6
		
		// Feedbacks have greater radius
		
		// L1
		cfg.addIntraLayerConfig(0, 1, new IntraLayersConnConfig((int)(1.0*connScale), (int)(3.0*connScale), 0.30f)); // 100 300
		cfg.addIntraLayerConfig(0, 2, new IntraLayersConnConfig((int)(0.1*connScale), (int)(0.6*connScale), 0.60f)); //  20  80
		// L2
		cfg.addIntraLayerConfig(1, 3, new IntraLayersConnConfig((int)(0.1*connScale), (int)(0.3*connScale), 0.50f)); // 100 300
		cfg.addIntraLayerConfig(1, 4, new IntraLayersConnConfig((int)(0.2*connScale), (int)(0.5*connScale), 0.85f)); //  30 100
		cfg.addIntraLayerConfig(1, 0, new IntraLayersConnConfig((int)(0.2*connScale), (int)(0.8*connScale), 0.50f)); //  20  80
		// L3
		cfg.addIntraLayerConfig(2, 3, new IntraLayersConnConfig((int)(1.5*connScale), (int)(4.0*connScale), 0.25f)); // 150 400
		cfg.addIntraLayerConfig(2, 1, new IntraLayersConnConfig((int)(0.5*connScale), (int)(1.5*connScale), 0.50f)); //  50 150
		cfg.addIntraLayerConfig(2, 4, new IntraLayersConnConfig((int)(0.1*connScale), (int)(0.5*connScale), 0.40f)); //  20  80
		// L4
		cfg.addIntraLayerConfig(3, 4, new IntraLayersConnConfig((int)(1.0*connScale), (int)(3.0*connScale), 0.25f)); // 100 300
		cfg.addIntraLayerConfig(3, 2, new IntraLayersConnConfig((int)(0.1*connScale), (int)(0.4*connScale), 0.50f)); //  50 150
		cfg.addIntraLayerConfig(3, 1, new IntraLayersConnConfig((int)(0.1*connScale), (int)(0.4*connScale), 0.60f)); //  20  80
		cfg.addIntraLayerConfig(3, 5, new IntraLayersConnConfig((int)(0.2*connScale), (int)(0.8*connScale), 0.40f)); //  20  80
		// L5
		cfg.addIntraLayerConfig(4, 3, new IntraLayersConnConfig((int)(0.3*connScale), (int)(1.0*connScale), 0.50f)); //  30 100
		cfg.addIntraLayerConfig(4, 2, new IntraLayersConnConfig((int)(0.2*connScale), (int)(0.8*connScale), 0.80f)); //  20  80
		// L6
		cfg.addIntraLayerConfig(5, 3, new IntraLayersConnConfig((int)(1.0*connScale), (int)(3.0*connScale), 1.00f)); // 100 300
		cfg.addIntraLayerConfig(5, 2, new IntraLayersConnConfig((int)(0.8*connScale), (int)(2.0*connScale), 1.00f)); //  80 200
		cfg.addIntraLayerConfig(5, 1, new IntraLayersConnConfig((int)(0.5*connScale), (int)(1.5*connScale), 1.00f, true )); //  50 150  
		cfg.addIntraLayerConfig(5, 0, new IntraLayersConnConfig((int)(0.1*connScale), (int)(0.4*connScale), 1.00f, true )); //  20  80
		
		return cfg;
	}
}
