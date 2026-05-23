package com.cortex;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import com.cortex.base.Synapse;
import com.cortex.base.config.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.config.InhibitorySynapticPlasticityConfig;
import com.cortex.base.config.SynapsePlasticityConfig;
import com.cortex.brain.Brain;
import com.cortex.brain.BrainLayersConfig;
import com.cortex.brain.BrainLayersConnConfig;
import com.cortex.classifiers.ocr.OCRCharacterNeuron;
import com.cortex.classifiers.ocr.OCRClassifier;
import com.cortex.classifiers.ocr.OCRSupervisor;
import com.cortex.commons.modules.ISensor;
import com.cortex.commons.modules.ISupervisor;
import com.cortex.globals.GlobalContext;
import com.cortex.globals.NeuralEngine;
import com.cortex.sensors.retina.Retina;
import com.cortex.sensors.retina.RetinaConfig;
import com.cortex.sensors.retina.RetinaNeuronConfig;

public class Boostrap {

	public static Brain buildBrain( int totalNeurons, int connScale ) {
		BrainLayersConfig layersCfg = new BrainLayersConfig();	
		
		return Brain.newBuilder()
			.addLayerConfig(layersCfg.l0_config((int)(totalNeurons*0.15f), 5*connScale,  7*connScale, 0.60f))
			.addLayerConfig(layersCfg.l1_config((int)(totalNeurons*0.20f), 8*connScale, 12*connScale, 0.50f))
			.addLayerConfig(layersCfg.l2_config((int)(totalNeurons*0.25f),10*connScale, 15*connScale, 0.40f))
			.addLayerConfig(layersCfg.l3_config((int)(totalNeurons*0.20f),12*connScale, 18*connScale, 0.30f))
			.addLayerConfig(layersCfg.l4_config((int)(totalNeurons*0.12f), 7*connScale, 10*connScale, 0.20f))
			.addLayerConfig(layersCfg.l5_config((int)(totalNeurons*0.08f), 8*connScale, 12*connScale, 0.10f))
			.addLayerConnectionConfig( new BrainLayersConnConfig(connScale) )
			.withTotalNeurons(totalNeurons)
			.build();
	}

	public static ISensor buildAndConnectRetina( Brain brain ) throws IOException {
		Retina retina = new Retina(
				RetinaConfig.newBuilder(70, 70).build(),
				RetinaNeuronConfig.newBuilder().build()
				);
		retina.setImage( loadImage("src/main/resources/Letter-A.png"));

		// Connect retina to L0
		brain.getSensorsTargetLayer().link(retina, 10, 40, 0.5f, 
				Synapse.SKIP_INHIBITOR_CONNECT_PREDICATE, 
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
		return retina;
	}

	public static ISupervisor<OCRCharacterNeuron> buildAndConnectOCR( Brain brain ) {
		OCRClassifier ocrClassifier = new OCRClassifier();		
		brain.getClassifiersSourceLayer().link( ocrClassifier, 10, 40, 0.5f, 
				Synapse.SKIP_INHIBITOR_CONNECT_PREDICATE, 
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
		return ocrSupervisor;
	}

	public static void main(String[] args) throws IOException, InterruptedException { 
		int totalNeurons = 40_000;
		int fanOut = 150;
		int connScale = (fanOut>=1000)?100:(fanOut>=100)?10:1;

		// Create Brain
		Brain brain = buildBrain(totalNeurons, connScale);
		System.out.println("Number of Neurons:"+brain.getNeuronsCount());
		System.out.println("Number of Synapses:"+brain.getSynapsesCount());

		// =======================================================================================
		// Create sensors
		ISensor retina = buildAndConnectRetina( brain );
		System.out.println("Number of Retina Synapses:"+retina.getSynapsesCount());

		// =======================================================================================
		// Connect OCR Classifier to L4
		ISupervisor<OCRCharacterNeuron> ocrSupervisor = buildAndConnectOCR( brain );		
		System.out.println("Number of OCR Synapses:"+ocrSupervisor.getClassifier().getSynapsesCount());

		GlobalContext.setMultiSphericalLayer(brain); // TODO:REMOVE ME
		// =======================================================================================
		// This is the main processing loop
		NeuralEngine thinker = new NeuralEngine( brain );
		thinker.attachSensor( retina );
		thinker.attachClassifier( ocrSupervisor.getClassifier() );
		thinker.attachSupervisor( ocrSupervisor );

		CountDownLatch keepAlive = new CountDownLatch(1);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutting down...");
			try {
				retina.stop();
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
