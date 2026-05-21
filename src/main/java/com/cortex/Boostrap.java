package com.cortex;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.cortex.base.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.InhibitorySynapticPlasticityConfig;
import com.cortex.base.Neuron;
import com.cortex.brain.GlobalContext;
import com.cortex.brain.Thinker;
import com.cortex.classifiers.ocr.OCRClassifier;
import com.cortex.classifiers.ocr.OCRSupervisor;
import com.cortex.layer.MultiLayerConfig.IntraLayersConnConfig;
import com.cortex.layer.MultiSphericalLayer;
import com.cortex.layer.MultiSphericalLayerConfig;
import com.cortex.layer.SphericalLayerConfig;
import com.cortex.layer.SynapsePlasticityConfig;
import com.cortex.sensors.retina.Retina;

public class Boostrap {

	public static void main(String[] args) throws IOException, InterruptedException { 
		int totalNeurons = 40_000;
		int fanOut = 150;
		int connScale = (fanOut>=1000)?100:(fanOut>=100)?10:1;

		GlobalContext.initialize(totalNeurons);
		
		// Create network layers and synapses
		MultiSphericalLayerConfig cfg = buildMultiSphericalLayerConfig( totalNeurons, connScale );
		MultiSphericalLayer layer = new MultiSphericalLayer( cfg );

		System.out.println("Number of Neurons:"+layer.getNeuronsCount());
		System.out.println("Number of Synapses:"+layer.getSynapsesCount());

		GlobalContext.setMultiSphericalLayer(layer);
		
		// Create sensors
		Retina retina = new Retina(70 , 70);
		retina.setImage( loadImage("src/main/resources/Letter-A.png"));

		// Connect retina to L1
		int retinaConn = layer.getLayers(0).link(retina, 10, 40, 0.5f, 
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
	private static final Predicate<Neuron> ALWAYS_CONNECT_PREDICATE = n -> true;
	private static final Predicate<Neuron> SKIP_INHIBITOR_CONNECT_PREDICATE = n -> !n.isInhibitor();
	private static final Predicate<Neuron> ONLY_INHIBITOR_CONNECT_PREDICATE = Neuron::isInhibitor;
	
	public static SynapsePlasticityConfig ffPlasticity() {
	    return new SynapsePlasticityConfig(
	    	ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.20f, 0.30f, 0.09f, 0.25f)// INITIAL, W_MAX, W_MIN, W_BASELINE
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
	    );
	}

	public static SynapsePlasticityConfig fbPlasticity() {
		return new SynapsePlasticityConfig(
			ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
				.withPlasticity(80f, 10f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.001f) 			// HOMEOSTATIC_RATE
				.withInitialDelay(1.2f)
				.build(),
			InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.00005f)				// LEARNING_RATE
				.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
				.build()
	    );
	}

	public static SynapsePlasticityConfig ctrlPlasticity() {
		return new SynapsePlasticityConfig(
			ExcitatorySynapticPlasticityConfig.newBuilder()
				.withSTDP(0.004f, 0.003f, 150_000_000L, 300_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
				.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
				.withEligibility(0.995f) 				// ELIGIBILITY_DECAY
				.withPlasticity(150f, 20f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
				.withHomeostaticRate(0.001f) 			// HOMEOSTATIC_RATE
				.withInitialDelay(1.2f)
				.build(),
			InhibitorySynapticPlasticityConfig.newBuilder()
				.withWeights(0.80f, 1.5f, 0.2f)			// INITIAL, W_MAX, W_MIN,
				.withLearningRate(0.00005f)				// LEARNING_RATE
				.withTargetFiringRate(4.0f)				// TARGET_FIRING_RATE
				.build()
		   );
	}
	
	private static MultiSphericalLayerConfig buildMultiSphericalLayerConfig( int totN, int connScale) {
		MultiSphericalLayerConfig cfg = new MultiSphericalLayerConfig();

		// L0 
		cfg.addLayerConfig( 
			new SphericalLayerConfig((int)(totN*0.15), 0.15f,  5*connScale, 7*connScale, 0.60f, 1.00f, true, true,
				ALWAYS_CONNECT_PREDICATE,
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.20f, 0.3f, 0.1f, 0.2f)	// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibility(0.99f) 				// ELIGIBILITY_DECAY
						.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
						.withInitialDelay(1.2f)
						.build(),			
					InhibitorySynapticPlasticityConfig.newBuilder()
						.withWeights(0.80f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
						.withLearningRate(0.005f)				// LEARNING_RATE
						.withTargetFiringRate(2.5f)				 // TARGET_FIRING_RATE
						.build()
		)));
		
		// L1 
		cfg.addLayerConfig( 
			new SphericalLayerConfig((int)(totN*0.20), 0.25f,  8*connScale, 12*connScale, 0.50f, 0.85f, true, true,
				ALWAYS_CONNECT_PREDICATE,
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.002f, 0.002f, 50_000_000L, 50_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.20f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
						.withPlasticity(40f, 5f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.002f) 			// HOMEOSTATIC_RATE
						.withInitialDelay(1.0f)
						.build(),			
					InhibitorySynapticPlasticityConfig.newBuilder()
						.withWeights(0.50f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
						.withLearningRate(0.005f)				// LEARNING_RATE
						.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
						.build()
		)));						

		// L2 
		cfg.addLayerConfig( 
			new SphericalLayerConfig((int)(totN*0.25), 0.15f, 10*connScale, 15*connScale, 0.40f, 0.65f, true, true,
				ALWAYS_CONNECT_PREDICATE,
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.20f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
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
		)));			
		
		// L3 
		cfg.addLayerConfig( 
			new SphericalLayerConfig((int)(totN*0.20), 0.30f, 12*connScale, 18*connScale, 0.30f, 0.45f, true, true,
				ALWAYS_CONNECT_PREDICATE,
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.004f, 0.003f, 80_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.20f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibility(0.990f) 				// ELIGIBILITY_DECAY
						.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
						.withInitialDelay(1.2f)
						.build(),			
					InhibitorySynapticPlasticityConfig.newBuilder()
						.withWeights(0.60f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
						.withLearningRate(0.005f)				// LEARNING_RATE
						.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
						.build()
		)));

		//L4 
		cfg.addLayerConfig( 
			new SphericalLayerConfig((int)(totN*0.12), 0.10f, 7*connScale, 10*connScale, 0.20f, 0.30f, true, true,
				ALWAYS_CONNECT_PREDICATE,
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.15f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibility(0.985f) 				// ELIGIBILITY_DECAY
						.withPlasticity(80f, 10f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.002f) 			// HOMEOSTATIC_RATE
						.withInitialDelay(1.5f)
						.build(),			
					InhibitorySynapticPlasticityConfig.newBuilder()
						.withWeights(0.60f, 3.0f, 0.2f)			// INITIAL, W_MAX, W_MIN,
						.withLearningRate(0.005f)				// LEARNING_RATE
						.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
						.build()
		)));

		//L5 
		cfg.addLayerConfig( 
			new SphericalLayerConfig((int)(totN*0.08), 0.20f, 8*connScale, 12*connScale, 0.10f, 0.15f, true, true,
				ALWAYS_CONNECT_PREDICATE,
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.004f, 0.003f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.12f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibility(0.99f) 				// ELIGIBILITY_DECAY
						.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
						.withInitialDelay(1.5f)
						.build(),			
					InhibitorySynapticPlasticityConfig.newBuilder()
						.withWeights(0.2f, 3.0f, 0.15f)			// INITIAL, W_MAX, W_MIN,
						.withLearningRate(0.005f)				// LEARNING_RATE
						.withTargetFiringRate(2.5f)				// TARGET_FIRING_RATE
						.build()
		)));
		
		// ==========================================================================================

		// L0 -> L1		
		cfg.addIntraLayerConfig(0, 1, new IntraLayersConnConfig(
				(int)(1.5*connScale), (int)(3.0*connScale), 
				0.45f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L0 -> L2
		cfg.addIntraLayerConfig(0, 2, new IntraLayersConnConfig(
				(int)(0.6*connScale), (int)(1.2*connScale), 
				0.85f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L1 -> L2
		cfg.addIntraLayerConfig(1, 2, new IntraLayersConnConfig(
				(int)(0.4*connScale), (int)(0.6*connScale), 
				0.50f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L1 -> L3
		cfg.addIntraLayerConfig(1, 3, new IntraLayersConnConfig(
				(int)(0.5*connScale), (int)(0.80*connScale), 
				0.9f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L1 -> L4
		cfg.addIntraLayerConfig(1, 4, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.5*connScale), 
				0.85f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L1 -> L0
		cfg.addIntraLayerConfig(1, 0, new IntraLayersConnConfig(
				(int)(0.3*connScale), (int)(0.5*connScale), 
				0.50f, // max distance
				fbPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L2 -> L3
		cfg.addIntraLayerConfig(2, 3, new IntraLayersConnConfig(
				(int)(1.5*connScale), (int)(4.0*connScale), 
				0.50f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L2 -> L1
		cfg.addIntraLayerConfig(2, 1, new IntraLayersConnConfig(
				(int)(0.5*connScale), (int)(1.5*connScale), 
				0.50f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L2 -> L4
		cfg.addIntraLayerConfig(2, 4, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.6*connScale), 
				0.50f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L3 -> L4
		cfg.addIntraLayerConfig(3, 4, new IntraLayersConnConfig(
				(int)(1.0*connScale), (int)(3.0*connScale), 
				0.25f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L3 -> L2
		cfg.addIntraLayerConfig(3, 2, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.4*connScale), 
				0.50f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L3 -> L1
		cfg.addIntraLayerConfig(3, 1, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.4*connScale), 
				0.60f, // max distance
				fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L3 -> L5
		cfg.addIntraLayerConfig(3, 5, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.40f,
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L4 -> L3
		//cfg.addIntraLayerConfig(4, 3, new IntraLayersConnConfig(
		//		(int)(0.3*connScale), (int)(1.0*connScale), 
		//		0.50f, // max distance
		//		ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L4 -> L2
		cfg.addIntraLayerConfig(4, 2, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.80f, // max distance
				fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L5 -> L3
		cfg.addIntraLayerConfig(5, 3, new IntraLayersConnConfig(
				(int)(1.0*connScale), (int)(3.0*connScale), 
				1.0f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L5 -> L2
		cfg.addIntraLayerConfig(5, 2, new IntraLayersConnConfig(
				(int)(0.8*connScale), (int)(2.0*connScale), 
				1.0f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L5 -> L1
		cfg.addIntraLayerConfig(5, 1, new IntraLayersConnConfig(
				(int)(0.5*connScale), (int)(1.5*connScale), 
				1.0f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L5 -> L0
		cfg.addIntraLayerConfig(5, 0, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.4*connScale), 
				1.0f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));

		
		// L3 -> L4
		cfg.addIntraLayerConfig(3, 4, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.40f,
				ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
		// L4 -> L5
		cfg.addIntraLayerConfig(4, 5, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.40f,
				ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
		// L3 -> L3
		cfg.addIntraLayerConfig(3, 3, new IntraLayersConnConfig(
			    (int)(0.2*connScale), (int)(0.6*connScale),
			    0.40f,
			    ffPlasticity(), ALWAYS_CONNECT_PREDICATE));
		
		return cfg;
	}
}
