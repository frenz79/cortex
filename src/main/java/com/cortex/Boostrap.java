package com.cortex;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cortex.base.Commons;
import com.cortex.base.Synapse;
import com.cortex.base.modules.ISensor;
import com.cortex.base.modules.ISupervisor;
import com.cortex.base.plasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.plasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.base.plasticity.SynapsePlasticityConfig;
import com.cortex.brain.Brain;
import com.cortex.brain.BrainLayersConfig;
import com.cortex.brain.BrainLayersConnConfig;
import com.cortex.externals.classifiers.ocr.OCRCharacterNeuron;
import com.cortex.externals.classifiers.ocr.OCRClassifier;
import com.cortex.externals.classifiers.ocr.OCRSupervisor;
import com.cortex.externals.sensors.retina.Retina;
import com.cortex.externals.sensors.retina.RetinaConfig;
import com.cortex.externals.sensors.retina.RetinaNeuronConfig;
import com.cortex.globals.DiscreteAdaptiveStabilizer;
import com.cortex.globals.DiscreteAdaptiveStabilizerConfig;
import com.cortex.globals.DiscreteAdaptiveStabilizerConfig.LayerAdaptiveParams;
import com.cortex.globals.NeuralEngine;
import com.cortex.globals.NeuralEngineConfig;
import com.cortex.metrics.MetricsRecorder;
import com.cortex.viewer.PointMeshViewerFX;

/**
 * How to launch:
 * CMD args:
 *  +ui -> to show cockpit
 * VM Args
 * -XX:+UseCompactObjectHeaders
 * -Xmx24000m
 * --module-path D:/SourceCode/Incubator/cortex/lib/javafx/
 * --add-modules javafx.controls,javafx.fxml,javafx.graphics
 * --enable-native-access=ALL-UNNAMED
 * --enable-native-access=javafx.graphics
 * --add-modules javafx.controls,javafx.base,javafx.fxml,javafx.graphics,javafx.media,javafx.web 
 * --add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED 
 * --add-exports javafx.base/com.sun.javafx.event=ALL-UNNAMED
 * 
 * 
 */
public class Boostrap {

	private final static Logger logger = LogManager.getLogger(Boostrap.class);
	
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
		brain.getSensorsTargetLayer().link(retina, 4500, 5000, 0.5f, 
				Commons.SKIP_INHIBITOR_CONNECT_PREDICATE, 
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.0015f, 0.0025f, 40_000_000L, 80_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.04f, 0.12f, 0.01f, 0.08f)// INITIAL, W_MAX, W_MIN, W_BASELINE
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
				);
		return retina;
	}

	public static ISupervisor<OCRCharacterNeuron> buildAndConnectOCR( Brain brain ) {
		OCRClassifier ocrClassifier = new OCRClassifier();		
		brain.getClassifiersSourceLayer().link( ocrClassifier, 10, 40, 2.5f, 
				Commons.SKIP_INHIBITOR_CONNECT_PREDICATE, 
				new SynapsePlasticityConfig(
					ExcitatorySynapticPlasticityConfig.newBuilder()
						.withSTDP(0.002f, 0.002f, 80_000_000L, 150_000_000L) // A_PLUS, A_MINUS, TAU_PLUS, TAU_MINUS
						.withWeights(0.11f, 0.30f, 0.10f, 0.20f)// INITIAL, W_MAX, W_MIN, W_BASELINE
						.withEligibilityDecaySeconds(0.990f) 	// ELIGIBILITY_DECAY
						.withPlasticity(20f, 1f)				// PLASTIC_DELAY_MAX, PLASTIC_DELAY_MIN
						.withHomeostaticRate(0.01f) 			// HOMEOSTATIC_RATE
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
		int connScale = 50;

		// Create Brain
		Brain brain = buildBrain(totalNeurons, connScale);
		logger.info("Number of Neurons:{}",brain.getNeuronsCount());
		logger.info("Number of Synapses:{}",brain.getSynapsesCount());

		// Create sensors
		ISensor retina = buildAndConnectRetina( brain );
		logger.info("Number of Retina Synapses:{}",retina.getSynapsesCount());

		// Connect OCR Classifier to L4
		ISupervisor<OCRCharacterNeuron> ocrSupervisor = buildAndConnectOCR( brain );		
		logger.info("Number of OCR Synapses{}",ocrSupervisor.getClassifier().getSynapsesCount());

		// Stabilizer
		DiscreteAdaptiveStabilizer stabilizer = new DiscreteAdaptiveStabilizer( brain,
			DiscreteAdaptiveStabilizerConfig.newBuilder()
			.addLayerParams(0, new LayerAdaptiveParams(
			        0.10f, 0.30f,		// TARGET_FIRING_LOW - HIGH
			        0.90f, 0.98f,		// TARGET_SPARSITY_MIN - HIGH
			        150_000f			// MAX_ENERGY
			    ))
			    .addLayerParams(1, new LayerAdaptiveParams(
			        0.05f, 0.15f,		// TARGET_FIRING_LOW - HIGH
			        0.95f, 0.995f,		// TARGET_SPARSITY_MIN - HIGH
			        50_000f				// MAX_ENERGY
			    ))
			    .addLayerParams(2, new LayerAdaptiveParams(
			        0.02f, 0.10f,		// TARGET_FIRING_LOW - HIGH
			        0.97f, 0.999f,		// TARGET_SPARSITY_MIN - HIGH
			        20_000f				// MAX_ENERGY
			    ))
			    .addLayerParams(3, new LayerAdaptiveParams(
			        0.01f, 0.05f,		// TARGET_FIRING_LOW - HIGH
			        0.98f, 0.9995f,		// TARGET_SPARSITY_MIN - HIGH
			        10_000f
			    ))
			    .addLayerParams(4, new LayerAdaptiveParams(
			        0.005f, 0.02f,		// TARGET_FIRING_LOW - HIGH
			        0.985f, 0.9997f,	// TARGET_SPARSITY_MIN - HIGH
			        5_000f
			    ))
			    .addLayerParams(5, new LayerAdaptiveParams(
			        0.001f, 0.01f,		// TARGET_FIRING_LOW - HIGH
			        0.99f, 0.9998f,		// TARGET_SPARSITY_MIN - HIGH
			        3_000f				// MAX_ENERGY
			    ))
				.build()
		);
		
		MetricsRecorder metricsRecorder = new MetricsRecorder(brain, 5000l);
		
		// =======================================================================================
		// This is the main processing loop
		NeuralEngine engine = new NeuralEngine( brain, new NeuralEngineConfig() );
		engine.withMetricsRecorder( metricsRecorder );

		brain.attachSensor( retina );
		brain.attachClassifier( ocrSupervisor.getClassifier() );
		brain.attachSupervisor( ocrSupervisor );
		brain.compact();
		
		CountDownLatch keepAlive = new CountDownLatch(1);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Shutting down...");
			try {
				retina.stop();
			} catch (Exception ignored) {}
			try {
				engine.stop();
			} catch (Exception ignored) {}
			keepAlive.countDown();
		}));

		// Open UI
		if (args.length>0 && args[0].equals("+ui")) {
			PointMeshViewerFX.launchViewer(brain, retina);
		}
		
		// ..give the life!
		engine.start();
		Thread.sleep(200); // breve delay per garantire che il thinker sia operativo

		retina.start();

		keepAlive.await();
		logger.info("Main exiting");
	}

	private static BufferedImage loadImage(String path) throws IOException {
		return ImageIO.read(new File(path));
	}
}
