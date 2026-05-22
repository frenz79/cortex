package com.cortex.viewer;

import java.awt.BorderLayout;
import java.util.concurrent.TimeUnit;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

import com.cortex.base.config.LayerConfig;
import com.cortex.brain.CorticalNeuron;
import com.cortex.brain.layers.Layer;
import com.cortex.brain.layers.MultiLayer;
import com.cortex.brain.layers.MultiLayerConfig;
import com.cortex.globals.GlobalContext;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class SimpleViewer<MC extends MultiLayerConfig<C>, C extends LayerConfig, L extends Layer<C>> extends JFrame {

	private final MultiLayer<MC,C,L> layers;

	public SimpleViewer( MultiLayer<MC,C,L> layers, boolean drawNeurons, boolean drawSynapses ) {
		super("Simple Viewer");
		System.out.println(" ------------------------------------------- ");
		this.layers = layers;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);

		Canvas3D canvas3D = new Canvas3D(SimpleUniverse.getPreferredConfiguration(), false);
		add(new JScrollPane(canvas3D), BorderLayout.CENTER);

		SimpleUniverse simpleUniverse = new SimpleUniverse(canvas3D);
		simpleUniverse.getViewingPlatform().setNominalViewingTransform();

		BranchGroup scene = createSceneGraph(drawNeurons, drawSynapses);
		simpleUniverse.addBranchGraph(scene);

		setVisible(true);
	}

	private Color3f getGradient(int id, int steps, Color3f start, Color3f end ) {
		float ratio = (float) id / (float) steps;
        int red = (int) (start.x * ratio + end.x * (1 - ratio));
        int green = (int) (start.y * ratio + end.y * (1 - ratio));
        int blue = (int) (start.z * ratio + end.z * (1 - ratio));        
		return new Color3f(red, green, blue);
	}
	
	private BranchGroup createSceneGraph( boolean drawNeurons, boolean drawSynapses ) {
		TransformGroup objRotate = new TransformGroup();
		objRotate.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		if ( drawNeurons) {
			registerNeuronLayers( objRotate );
		}
		if ( drawSynapses ) {
		//	registerSynapses( objRotate );
		}

		BranchGroup branchGroup = new BranchGroup();
		registerMouseListener( branchGroup, objRotate );
		branchGroup.addChild(objRotate);
		return branchGroup;
	}
	
	private void registerNeuronLayers( TransformGroup objRotate ) {
		long startTime = System.nanoTime();
		for ( L layer : layers.getAllLayers() ) {
			Shape3D layer3d = buildNeuronsShape( 
				layer, 
				getGradient(layer.getId(), 
					layers.getAllLayers().size(), 
					new Color3f(255.0f, 0.0f, 0.0f), 
					new Color3f(0.0f, 255.0f, 255.0f)) );
			objRotate.addChild(layer3d);
		}

		long endTime = System.nanoTime();
		System.out.println("buildNeuronsShape() time:"+TimeUnit.NANOSECONDS.toMicros(endTime-startTime));
	}
	/*
	private void registerSynapses( TransformGroup objRotate ) {
		long startTime = System.nanoTime();
		for ( L layer : layers.getAllLayers() ) {
			Shape3D synapses3d = buildSynapsesShape( layer, new Color3f(255.0f, 0.0f, 0.0f) );
			if (synapses3d!=null) {
				objRotate.addChild(synapses3d);
			}
		}
		long endTime = System.nanoTime();
		System.out.println("buildSynapsesShape() time:"+TimeUnit.NANOSECONDS.toMicros(endTime-startTime));
	}
	*/
	private void registerMouseListener( BranchGroup branchGroup, TransformGroup objRotate ) {
		MouseRotate mr = new MouseRotate();
		mr.setTransformGroup(objRotate);
		mr.setSchedulingBounds(new BoundingSphere());
		branchGroup.addChild(mr);

		MouseWheelZoom mz = new MouseWheelZoom();
		mz.setTransformGroup(objRotate);
		mz.setSchedulingBounds(new BoundingSphere());
		branchGroup.addChild(mz);

		MouseTranslate msl = new MouseTranslate();
		msl.setTransformGroup(objRotate);
		msl.setSchedulingBounds(new BoundingSphere());
		branchGroup.addChild(msl);   
	}
	/*
	private Shape3D buildSynapsesShape(Layer<?> layer, Color3f color) {
		// Synapses
        List<Point3f> synapses = new ArrayList<>(layer.getSynapsesCount()*2);
        if ( layer.getSynapsesCount()>0) {	       
	        for ( LayeredNeuron n : layer.getNeurons() ) {
	        	for ( Synapse d : n.getInSynapses() ) {
	        		LayeredNeuron src = (LayeredNeuron)d.getSource();
	        		synapses.add( src.getPosition() );
		        	synapses.add( n.getPosition() );
	        	}
	        }
	        LineArray syn = new LineArray(synapses.size(), GeometryArray.COORDINATES);
	        syn.setCoordinates(0, synapses.toArray(new Point3f[0]));
	        return new Shape3D(syn, getSynapseLook(color));
        }
        return null;
	}
*/
	private Shape3D buildNeuronsShape( Layer layer, Color3f color ) {
		// Neurons
		Point3f[] plaPts = new Point3f[GlobalContext.getNeuronsCount()];
		int i = 0;

		for ( CorticalNeuron n : layer.getNeurons() ) {
			plaPts[i++] = n.getPosition();
		}

		PointArray pla = new PointArray(plaPts.length, GeometryArray.COORDINATES);
		pla.setCoordinates(0, plaPts);

		Shape3D plShape = new Shape3D(pla, getNeuronsLook(color));
		return plShape;
	}
	
	private Appearance getNeuronsLook(Color3f color) {
		Appearance appearance = new Appearance();
		ColoringAttributes coloringAttributes = new ColoringAttributes(
			color, ColoringAttributes.FASTEST
		);
		appearance.setColoringAttributes(coloringAttributes);

		PointAttributes attr = new PointAttributes(2.0f, false);
		appearance.setPointAttributes(attr);
		return appearance;
	}
	
	private Appearance getSynapseLook(Color3f color) {
		Appearance appearance = new Appearance();
		ColoringAttributes coloringAttributes = new ColoringAttributes(
			color, ColoringAttributes.FASTEST
		);		
		appearance.setColoringAttributes(coloringAttributes);
		
		LineAttributes attr = new LineAttributes(0.01f, LineAttributes.PATTERN_SOLID, true);
		appearance.setLineAttributes(attr);
		return appearance;
	}
}
