package com.cortex;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.config.InhibitorySynapticPlasticityConfig;
import com.cortex.base.config.LayerConfig;
import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.Brain;
import com.cortex.brain.layers.MultiLayerConfig.IntraLayersConnConfig;
import com.cortex.classifiers.ocr.OCRClassifier;
import com.cortex.classifiers.ocr.OCRSupervisor;
import com.cortex.globals.GlobalContext;
import com.cortex.globals.Thinker;
import com.cortex.layer.SphericalLayerConfig;
import com.cortex.sensors.retina.Retina;
import com.cortex.sensors.retina.RetinaConfig;
import com.cortex.sensors.retina.RetinaNeuronConfig;

public class Boostrap {

	public static void main(String[] args) throws IOException, InterruptedException { 
		int totalNeurons = 40_000;
		int fanOut = 150;
		int connScale = (fanOut>=1000)?100:(fanOut>=100)?10:1;
		
		// Create network layers and synapses
		MultiSphericalLayerConfig cfg = buildMultiSphericalLayerConfig( totalNeurons, connScale );
		Brain layer = new Brain( cfg );

		System.out.println("Number of Neurons:"+layer.getNeuronsCount());
		System.out.println("Number of Synapses:"+layer.getSynapsesCount());

		GlobalContext.setMultiSphericalLayer(layer);
		
		// Create sensors
		Retina retina = new Retina(
			RetinaConfig.newBuilder(70, 70).build(),
			RetinaNeuronConfig.newBuilder().build(),
		);
		retina.setImage( loadImage("src/main/resources/Letter-A.png"));

		// Connect retina to L1
		int retinaConn = layer.getLayers(0).link(retina, 10, 40, 0.5f, 
			LayerConfig.SKIP_INHIBITOR_CONNECT_PREDICATE, 
			new SynapsePlasticityConfig(
				ExcitatorySynapticPlasticityConfig.newBuilder()
					.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
					.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
					.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
					.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
					.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
					.withInitialDelay(1.2f)
					.build(),
				InhibitorySynapticPlasticityConfig.newBuilder()
					.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
					.withLearningRate(0.005f)				// LEARNING_RATE
					.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
					.build()
			    )	
			);

		System.out.println("Number of Retina Synapses:"+retinaConn);
		
		// Connect OCR Classifier to L4
		OCRClassifier ocrClassifier = new OCRClassifier();		
		int ocrConn = layer.getLayers(3).link( ocrClassifier, 10, 40, 0.5f, 
			SKIP_INHIBITOR_CONNECT_PREDICATE, 
			new SynapsePlasticityConfig(
			   	ExcitatorySynapticPlasticityConfig.newBuilder()
					.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
					.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
					.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
					.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
					.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
					.withInitialDelay(1.2f)
					.build(),
				InhibitorySynapticPlasticityConfig.newBuilder()
					.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
					.withLearningRate(0.005f)				// LEARNING_RATE
					.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
					.build()
			    )	
			);
		OCRSupervisor ocrSupervisor = new OCRSupervisor( ocrClassifier );
		ocrSupervisor.setExpected(ocrClassifier.getCharacterNeuronForLetter('A'));
		
		System.out.println("Number of OCR Synapses:"+ocrConn);

		// This is the main processing loop
		Thinker<?,?,?> thinker = new Thinker<>( layer );
		thinker.attachSensor( retina );
		thinker.attachClassifier( ocrClassifier );
		thinker.attachSupervisor( ocrSupervisor );

		 CountDownLatch keepAlive = new CountDownLatch(1);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
		    System.out.println("Shutting down...");
		    try {
		        retina.stop(); // se disponibile
		    } catch (Exception ignored) {}
		    try {
		        thinker.stop();
		    } catch (Exception ignored) {}
		    keepAlive.countDown();
		}));
		
		// Open UI
		// new SimpleViewer( layer, true, false );

		// ..give the life!
		thinker.start();
		Thread.sleep(200); // breve delay per garantire che il thinker sia operativo
		
		retina.start();
		
	    keepAlive.await();
	    System.out.println("Main exiting");
	}

	private static BufferedImage loadImage(String path) throws IOException {
		return ImageIO.read(new File(path));
	}
}
