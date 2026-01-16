package com.cortex;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.ExcitatorySynapticPlasticity;
import com.cortex.base.ExcitatorySynapticPlasticity.ExcitatorySynapticPlasticityConfig;
import com.cortex.base.InhibitorySynapticPlasticity;
import com.cortex.base.InhibitorySynapticPlasticity.InhibitorySynapticPlasticityConfig;
import com.cortex.brain.Thinker;
import com.cortex.classifiers.ocr.OCRClassifier;
import com.cortex.layer.MultiLayerConfig.IntraLayersConnConfig;
import com.cortex.layer.MultiSphericalLayer;
import com.cortex.layer.MultiSphericalLayerConfig;
import com.cortex.layer.SphericalLayerConfig;
import com.cortex.layer.SynapsePlasticityConfig;
import com.cortex.sensors.retina.Retina;

public class Boostrap {

	public static void main(String[] args) throws IOException { 
		int totalNeurons = 1_000;
		int fanOut = 500;
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
		int retinaConn = layer.getLayers(0).connectSensor(
				retina.getNeurons(), 
				10, 
				40, 
				0.5f, 
				true,
				SKIP_INHIBITOR_CONNECT_PREDICATE, 
				new SynapsePlasticityConfig(
						new ExcitatorySynapticPlasticity(0.2f, 5.0f, FAST_EXCITATORY),
						new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
					)	
				);

		System.out.println("Number of Retina Synapses:"+retinaConn);
		
		// Connect OCR Classifier to L4
		OCRClassifier ocr = new OCRClassifier();		
		int ocrConn = layer.getLayers(3).connectSensor(
				ocr.getNeurons(), 
				10, 
				40, 
				0.5f, 
				false,
				SKIP_INHIBITOR_CONNECT_PREDICATE, 
				new SynapsePlasticityConfig(
						new ExcitatorySynapticPlasticity(0.2f, 5.0f, FAST_EXCITATORY),
						new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
					)	
				);
			
		System.out.println("Number of OCR Synapses:"+ocrConn);

		// This is the main processing loop
		Thinker<?,?,?> thinker = new Thinker<>( layer );
		thinker.attachSensor( retina );
		thinker.attachClassifier( ocr );

		// Open UI
		// new SimpleViewer( layer, true, false );

		// ..give the life!
		retina.start();
		thinker.start();
	}

	private static BufferedImage loadImage(String path) throws IOException {
		return ImageIO.read(new File(path));
	}

	private static final Predicate<AbstractNeuron> ALWAYS_CONNECT_PREDICATE = new Predicate<AbstractNeuron>() {
		@Override
		public boolean test(AbstractNeuron n) {
			return true;
		}
	};

	private static final Predicate<AbstractNeuron> SKIP_INHIBITOR_CONNECT_PREDICATE = new Predicate<AbstractNeuron>() {
		@Override
		public boolean test(AbstractNeuron n) {
			return !n.isInhibitor();
		}
	};

	private static final Predicate<AbstractNeuron> ONLY_INHIBITOR_CONNECT_PREDICATE = new Predicate<AbstractNeuron>() {
		@Override
		public boolean test(AbstractNeuron n) {
			return n.isInhibitor();
		}
	};
	
	private static final InhibitorySynapticPlasticityConfig GENERIC_INHIBITORY = new InhibitorySynapticPlasticityConfig(
			0.0005f,        // LEARNING_RATE
			3.0f,           // TARGET_FIRING_RATE
			0.2f,           // W_MIN
			6.0f,           // W_MAX
			300_000_000L    // RATE_WINDOW
			);
	
	// FEED-FORWARD CORTICALE (L1–L4)
	private static final ExcitatorySynapticPlasticityConfig FF_EXCITATORY = new ExcitatorySynapticPlasticityConfig(
			0.005f,        // A_PLUS
			0.006f,        // A_MINUS
			40_000_000L,   // TAU_PLUS
			80_000_000L,   // TAU_MINUS
			0.1f,          // W_MIN
			3.0f,          // W_MAX
			0.4f,          // W_BASELINE
			0.97f,         // ELIGIBILITY_DECAY
			0.0002f,       // HOMEOSTATIC_RATE
			false,         // PLASTIC_DELAY
			5f,            // DELAY_MIN
			40f            // DELAY_MAX
			);

	// FEEDBACK / CONTESTUALE (L2↔L4, L4→L1)
	private static final ExcitatorySynapticPlasticityConfig FB_EXCITATORY = new ExcitatorySynapticPlasticityConfig(
			0.002f,        // A_PLUS ↓
			0.002f,        // A_MINUS
			80_000_000L,   // TAU_PLUS ↑
			150_000_000L,  // TAU_MINUS
			0.2f,
			2.5f,
			0.6f,
			0.985f,
			0.00005f,
			false,
			10f,
			80f
			);

	// CONTROLLO / DECISIONE (L5–L6)
	private static final ExcitatorySynapticPlasticityConfig CTRL_EXCITATORY =
			new ExcitatorySynapticPlasticityConfig(
					0.0005f,
					0.0005f,
					150_000_000L,
					300_000_000L,
					0.5f,
					5.0f,
					1.5f,
					0.995f,
					0.00002f,
					false,
					20f,
					150f
					);

	public static SynapsePlasticityConfig ffPlasticity() {
		return new SynapsePlasticityConfig(
				new ExcitatorySynapticPlasticity(0.2f, 5.0f, FF_EXCITATORY),
				new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
				);
	}

	public static SynapsePlasticityConfig fbPlasticity() {
		return new SynapsePlasticityConfig(
				new ExcitatorySynapticPlasticity(0.15f, 4.0f, FB_EXCITATORY),
				new InhibitorySynapticPlasticity(0.85f, GENERIC_INHIBITORY)
				);
	}

	public static SynapsePlasticityConfig ctrlPlasticity() {
		return new SynapsePlasticityConfig(
				new ExcitatorySynapticPlasticity(0.1f, 3.0f, CTRL_EXCITATORY),
				new InhibitorySynapticPlasticity(0.9f, GENERIC_INHIBITORY)
				);
	}

	public static ExcitatorySynapticPlasticityConfig FAST_EXCITATORY = new ExcitatorySynapticPlasticityConfig(
			0.01f,   		// A_PLUS,	
			0.012f, 		// A_MINUS,				
			20_000_000L, 	// TAU_PLUS,				
			40_000_000L,	// TAU_MINUS	
			0.05f, 			// W_MIN,	
			2.0f,  			// W_MAX,		
			0.2f,			// W_BASELINE,
			0.95f,			// ELIGIBILITY_DECAY,
			0.0005f,		// HOMEOSTATIC_RATE,
			false,			// PLASTIC_DELAY,		
			1f,				// DELAY_MIN,		
			20f				// DELAY_MAX	
			);

	public static ExcitatorySynapticPlasticityConfig STABLE_EXCITATORY = new ExcitatorySynapticPlasticityConfig(
			0.01f 	* 0.4f, // A_PLUS,	
			0.012f  * 0.4f, // A_MINUS,				
			20_000_000L *2, // TAU_PLUS,				
			40_000_000L,	// TAU_MINUS	
			0.05f, 			// W_MIN,	
			2.0f,  			// W_MAX,		
			0.2f + 0.2f,	// W_BASELINE,
			0.95f,			// ELIGIBILITY_DECAY,
			0.0005f *0.2f,	// HOMEOSTATIC_RATE,
			false,			// PLASTIC_DELAY,		
			1f,				// DELAY_MIN,		
			20f				// DELAY_MAX	
			);

	public static ExcitatorySynapticPlasticityConfig VERY_SLOW_EXCITATORY = new ExcitatorySynapticPlasticityConfig(
			0.01f 	* 0.1f, // A_PLUS,	
			0.012f  * 0.1f, // A_MINUS,				
			20_000_000L *4, // TAU_PLUS,				
			40_000_000L,	// TAU_MINUS	
			0.05f, 			// W_MIN,	
			2.0f,  			// W_MAX,		
			0.2f + 0.2f,	// W_BASELINE,
			0.95f,			// ELIGIBILITY_DECAY,
			0.0005f *0.05f,	// HOMEOSTATIC_RATE,
			false,			// PLASTIC_DELAY,		
			1f,				// DELAY_MIN,		
			20f				// DELAY_MAX	
			);

	private static MultiSphericalLayerConfig buildMultiSphericalLayerConfig( int totN, int connScale) {
		MultiSphericalLayerConfig cfg = new MultiSphericalLayerConfig();

		// L1 
		cfg.addLayerConfig( 
				new SphericalLayerConfig((int)(totN*0.15), 0.15f,  5*connScale, 7*connScale, 0.60f, 1.00f,
						ALWAYS_CONNECT_PREDICATE,
						new SynapsePlasticityConfig(
								new ExcitatorySynapticPlasticity(0.2f, 5.0f, FAST_EXCITATORY),
								new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
								)	
						));
		// L2 
		cfg.addLayerConfig( 
				new SphericalLayerConfig((int)(totN*0.20), 0.25f,  8*connScale, 12*connScale, 0.50f, 0.85f,
						ALWAYS_CONNECT_PREDICATE,
						new SynapsePlasticityConfig(
								new ExcitatorySynapticPlasticity(0.2f, 5.0f, FAST_EXCITATORY),
								new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
								)	
						));
		// L3 
		cfg.addLayerConfig( 
				new SphericalLayerConfig((int)(totN*0.25), 0.15f, 10*connScale, 15*connScale, 0.40f, 0.65f,
						ALWAYS_CONNECT_PREDICATE,
						new SynapsePlasticityConfig(
								new ExcitatorySynapticPlasticity(0.2f, 5.0f, STABLE_EXCITATORY),
								new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
								)	
						));
		// L4 
		cfg.addLayerConfig( 
				new SphericalLayerConfig((int)(totN*0.20), 0.30f, 12*connScale, 18*connScale, 0.30f, 0.45f,
						ALWAYS_CONNECT_PREDICATE,
						new SynapsePlasticityConfig(
								new ExcitatorySynapticPlasticity(0.2f, 5.0f, STABLE_EXCITATORY),
								new InhibitorySynapticPlasticity(0.8f, GENERIC_INHIBITORY)
								)	
						));
		// L5 
		cfg.addLayerConfig( 
				new SphericalLayerConfig((int)(totN*0.12), 0.10f, 7*connScale, 10*connScale, 0.20f, 0.30f,
						ALWAYS_CONNECT_PREDICATE,
						new SynapsePlasticityConfig(
								new ExcitatorySynapticPlasticity(0.35f, 5.0f, VERY_SLOW_EXCITATORY),
								new InhibitorySynapticPlasticity(0.80f, GENERIC_INHIBITORY)
								)	
						));
		// L6 
		cfg.addLayerConfig( 
				new SphericalLayerConfig((int)(totN*0.08), 0.20f, 8*connScale, 12*connScale, 0.10f, 0.15f,
						ALWAYS_CONNECT_PREDICATE,
						new SynapsePlasticityConfig(
								new ExcitatorySynapticPlasticity(0.35f, 5.0f, VERY_SLOW_EXCITATORY),
								new InhibitorySynapticPlasticity(0.80f, GENERIC_INHIBITORY)
								)	
						));

		// ==========================================================================================

		// L1 -> L2		
		cfg.addIntraLayerConfig(0, 1, new IntraLayersConnConfig(
				(int)(1.0*connScale), (int)(3.0*connScale), 
				0.30f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L1 -> L3
		cfg.addIntraLayerConfig(0, 2, new IntraLayersConnConfig(
				(int)(0.6*connScale), (int)(1.2*connScale), 
				0.85f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L2 -> L3
		cfg.addIntraLayerConfig(1, 2, new IntraLayersConnConfig(
				(int)(0.4*connScale), (int)(0.6*connScale), 
				0.50f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L2 -> L4
		cfg.addIntraLayerConfig(1, 3, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.3*connScale), 
				0.50f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L2 -> L5
		cfg.addIntraLayerConfig(1, 4, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.5*connScale), 
				0.85f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L2 -> L1
		cfg.addIntraLayerConfig(1, 0, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.50f, // max distance
				fbPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L3 -> L4
		cfg.addIntraLayerConfig(2, 3, new IntraLayersConnConfig(
				(int)(1.5*connScale), (int)(4.0*connScale), 
				0.50f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L3 -> L2
		cfg.addIntraLayerConfig(2, 1, new IntraLayersConnConfig(
				(int)(0.5*connScale), (int)(1.5*connScale), 
				0.50f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L3 -> L5
		cfg.addIntraLayerConfig(2, 4, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.5*connScale), 
				0.40f, // max distance
				ffPlasticity(),	ALWAYS_CONNECT_PREDICATE));
		// L4 -> L5
		cfg.addIntraLayerConfig(3, 4, new IntraLayersConnConfig(
				(int)(1.0*connScale), (int)(3.0*connScale), 
				0.25f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L4 -> L3
		cfg.addIntraLayerConfig(3, 2, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.4*connScale), 
				0.50f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L4 -> L1
		cfg.addIntraLayerConfig(3, 1, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.4*connScale), 
				0.60f, // max distance
				fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L4 -> L6
		cfg.addIntraLayerConfig(3, 5, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.40f,
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L5 -> L4
		cfg.addIntraLayerConfig(4, 3, new IntraLayersConnConfig(
				(int)(0.3*connScale), (int)(1.0*connScale), 
				0.50f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L5 -> L3
		cfg.addIntraLayerConfig(4, 2, new IntraLayersConnConfig(
				(int)(0.2*connScale), (int)(0.8*connScale), 
				0.80f, // max distance
				fbPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L6 -> L4
		cfg.addIntraLayerConfig(5, 3, new IntraLayersConnConfig(
				(int)(1.0*connScale), (int)(3.0*connScale), 
				1.0f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L6 -> L3
		cfg.addIntraLayerConfig(5, 2, new IntraLayersConnConfig(
				(int)(0.8*connScale), (int)(2.0*connScale), 
				1.0f, // max distance
				ctrlPlasticity(), ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L6 -> L2
		cfg.addIntraLayerConfig(5, 1, new IntraLayersConnConfig(
				(int)(0.5*connScale), (int)(1.5*connScale), 
				1.0f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));
		// L6 -> L1
		cfg.addIntraLayerConfig(5, 0, new IntraLayersConnConfig(
				(int)(0.1*connScale), (int)(0.4*connScale), 
				1.0f, // max distance
				fbPlasticity(),	ONLY_INHIBITOR_CONNECT_PREDICATE));

		return cfg;
	}
}
