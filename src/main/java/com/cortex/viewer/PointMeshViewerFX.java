package com.cortex.viewer;

import java.util.ArrayList;
import java.util.List;

import com.cortex.base.AbstractNeuron;
import com.cortex.base.Synapse;
import com.cortex.brain.Brain;
import com.cortex.commons.Point3f;

import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PointMeshViewerFX extends Application {

	private static Brain brain;

	private double mouseOldX, mouseOldY;
	private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
	private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
	private final Translate translate = new Translate(0, 0, -0.5);

	private final Group synapseLines = new Group();
	private final Sphere selectedSphere = new Sphere(0.02);
	private final PhongMaterial selectedMaterial = new PhongMaterial(Color.YELLOW);
	private final List<Sphere> neuronSpheres = new ArrayList<>();

	private final Tooltip dynamicTooltip = new Tooltip();
	private long lastTooltipUpdate = 0;
	
	private Group root3d;
	private SubScene subScene;

	private Set<Integer> visibleLayers = new HashSet<>();
	
	public static void launchViewer(Brain b) {
		brain = b;
		launch();
	}

	@Override
	public void start(Stage stage) {

		root3d = new Group();
		root3d.getTransforms().addAll(rotateX, rotateY, translate);

		selectedSphere.setMaterial(selectedMaterial);
		selectedSphere.setVisible(false);
		root3d.getChildren().add(selectedSphere);

		// buildNeuronSpheres();
		buildNeuronMesh();
		root3d.getChildren().addAll(synapseLines);

		subScene = new SubScene(root3d, 800, 600, true, SceneAntialiasing.BALANCED);
		subScene.setFill(Color.BLACK);

		Tooltip.install(subScene, dynamicTooltip);
		
		PerspectiveCamera camera = new PerspectiveCamera(true);
		camera.setNearClip(0.0001);
		camera.setFarClip(10);
		rotateX.setAngle(0.3);
		rotateY.setAngle(-180);
		translate.setZ(-4.34);

		subScene.setCamera(camera);

		CockpitPanel panel = new CockpitPanel(6); // numero layer

		panel.setOnLayerChange(visibleLayers -> {
		    for (var entry : layerMeshes.entrySet()) {
		        boolean visible = visibleLayers.contains(entry.getKey());
		        entry.getValue().setVisible(visible);
		    }
		});
		panel.setOnShowInSynChange(v -> showInputSynapses = v);
		panel.setOnShowOutSynChange(v -> showOutputSynapses = v);
		panel.setOnHighlightSensorsChange(v -> highlightSensorPaths = v);
		
		BorderPane pane = new BorderPane(subScene);
		pane.setLeft(panel);

		
		BorderPane pane = new BorderPane(subScene);
		Scene scene = new Scene(pane);

		enableMouseControls(scene);

		stage.setTitle("PointMesh Neural Viewer");
		stage.setScene(scene);
		stage.show();
	}

	
	private void updateVisibility() {
	    for (Sphere s : neuronSpheres) {
	        AbstractNeuron n = (AbstractNeuron) s.getUserData();
	        s.setVisible(visibleLayers.contains(n.getLayerId()));
	    }
	}

	
	private void enableMouseControls(Scene scene) {		
		subScene.setOnMouseMoved(e -> {			
			long now = System.nanoTime();
			if (now - lastTooltipUpdate < 30_000_000) return; // 30 ms
			
			lastTooltipUpdate = now;
			
		    PickResult pr = e.getPickResult();
		    if (pr == null) {
		        dynamicTooltip.hide();
		        return;
		    }
		
		    Point3D hitPoint = pr.getIntersectedPoint();
		    if (hitPoint == null) {
		        dynamicTooltip.hide();
		        return;
		    }
		
		    AbstractNeuron n = findClosestNeuron(hitPoint);
		
		    if (n == null) {
		        dynamicTooltip.hide();
		        return;
		    }
		
		    dynamicTooltip.setText(
		        "Neuron " + n.getIndex() +
		        "\nLayer: " + n.getLayerId() +
		        "\nType: " + n.getClass().getSimpleName() +
		        "\nInSyn: " + n.getInSynapses().size() +
		        "\nOutSyn: " + n.getOutSynapses().size()
		    );
		
		    dynamicTooltip.show(
		        subScene,
		        e.getScreenX() + 10,
		        e.getScreenY() + 10
		    );

			highlightNeuron(n);
		});
		
		scene.setOnMousePressed(e -> {
			mouseOldX = e.getSceneX();
			mouseOldY = e.getSceneY();
		});

		scene.setOnMouseDragged(e -> {
			double dx = e.getSceneX() - mouseOldX;
			double dy = e.getSceneY() - mouseOldY;

			if (e.getButton() == MouseButton.PRIMARY) {
				rotateY.setAngle(rotateY.getAngle() + dx * 0.1);
				rotateX.setAngle(rotateX.getAngle() - dy * 0.1);
			}
			if (e.getButton() == MouseButton.SECONDARY) {
				translate.setX(translate.getX() + dx * 0.1);
				translate.setY(translate.getY() + dy * 0.1);
			}

			mouseOldX = e.getSceneX();
			mouseOldY = e.getSceneY();
		});

		// CLICK 3D REALE
		subScene.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {

				PickResult pr = e.getPickResult();
				if (pr == null)
					return;

				// Works withe neuron meshes
				Point3D hitPoint = pr.getIntersectedPoint();
		        if (hitPoint == null) return;
		
		        // trova neurone più vicino
		        AbstractNeuron hit = findClosestNeuron(hitPoint);
		
		        if (hit != null) {
		            highlightNeuron(hit);
		            highlightSynapses(hit);
		        }
				
				/* works with neuron spheres
				Node node = pr.getIntersectedNode();
				if (node instanceof Sphere && node.getUserData() instanceof AbstractNeuron) {
					AbstractNeuron hit = (AbstractNeuron) node.getUserData();
					highlightNeuron(hit);
					highlightSynapses(hit);
				}
				*/
			}
		});

		scene.addEventFilter(ScrollEvent.SCROLL, e -> {
			translate.setZ(translate.getZ() + e.getDeltaY() * 0.02);
		});
	}

	// Picking with Neuron meshes
private AbstractNeuron findClosestNeuron(Point3D hitPoint) {
    AbstractNeuron best = null;
    double bestDist = Double.MAX_VALUE;

    // tolleranza selezione (adattiva)
	double camDist = Math.abs(translate.getZ());
	double tolerance = 0.02 * camDist;
    // double tolerance = 0.08;
    for (AbstractNeuron n : brain.getAllNeurons()) {
        Point3D p = neuronToLocal(n);
        double dist = p.distance(hitPoint);
		// Click on empty space
		if (bestDist > tolerance * 1.5) return null;
        if (dist < tolerance && dist < bestDist) {
            bestDist = dist;
            best = n;
        }
    }
    return best;
}

	
	private void highlightNeuron(AbstractNeuron n) {
		Point3D p = neuronToLocal(n);
		selectedSphere.setTranslateX(p.getX());
		selectedSphere.setTranslateY(p.getY());
		selectedSphere.setTranslateZ(p.getZ());
		selectedSphere.setVisible(true);
	}

	private Point3D neuronToLocal(AbstractNeuron n) {
		Point3f p = n.getPosition();
		Point3D meshSpace = new Point3D(-p.x(), p.y(), p.z());
		Point3D sceneSpace = root3d.localToScene(meshSpace);
		return root3d.sceneToLocal(sceneSpace);
	}

	private void highlightSynapses(AbstractNeuron n) {
		synapseLines.getChildren().clear();
		Point3D b = neuronToLocal(n);
		for (Synapse s : n.getInSynapses()) {
			if (s.getSource()!=null && s.getSource().getPosition()!=null) {
				Point3D a = neuronToLocal(s.getSource());
				Node line = makeConnection(a, b, Color.YELLOW);
				synapseLines.getChildren().add(line);
			}
		}
		
		for (Synapse s : n.getOutSynapses()) {
			if (s.getTarget()!=null && s.getTarget().getPosition()!=null) {
				Point3D a = neuronToLocal(s.getTarget());
				Node line = makeConnection(a, b, Color.BLUE);
				synapseLines.getChildren().add(line);
			}
		}		
	}

private void buildNeuronMesh() {
    Map<Integer, TriangleMesh> map = new HashMap<>();

    for (AbstractNeuron n : brain.getAllNeurons()) {
        int layer = n.getLayerId();
        map.putIfAbsent(layer, new TriangleMesh());
        TriangleMesh mesh = map.get(layer);

        Point3f p = n.getPosition();
        float x = -p.x();
        float y = p.y();
        float z = p.z();

        float size = 0.01f;
        int base = mesh.getPoints().size() / 3;

        mesh.getPoints().addAll(
            x, y, z,
            x + size, y, z,
            x, y + size, z
        );

        mesh.getTexCoords().addAll(0,0);
        mesh.getFaces().addAll(
            base, 0,
            base+1, 0,
            base+2, 0
        );
    }

    Group g = new Group();
    for (var entry : map.entrySet()) {
        MeshView mv = new MeshView(entry.getValue());
        mv.setMaterial(new PhongMaterial(getLayerColor(entry.getKey())));

        layerMeshes.put(entry.getKey(), mv);
        g.getChildren().add(mv);
    }
	root3d.getChildren().add(g);
}


	
	private void buildNeuronSpheres() {
		for (AbstractNeuron n : brain.getAllNeurons()) {
			Point3f p = n.getPosition();

			Sphere s = new Sphere(0.01); // raggio neurone
			s.setMaterial(new PhongMaterial(getLayerColor(n.getLayerId())));

			// stessa trasformazione della mesh precedente
			s.setTranslateX(-p.x());
			s.setTranslateY(p.y());
			s.setTranslateZ(p.z());
			
			// tooltip
			Tooltip t = new Tooltip(
				    "Neuron " + n.getIndex() +
				    "\nLayer: " + n.getLayerId() +
				    "\nType: " + n.getClass().getSimpleName() +
				    "\nInSyn: " + n.getInSynapses().size() +
				    "\nOutSyn: " + n.getOutSynapses().size()
				);
			t.setShowDelay(Duration.ZERO);
			t.setHideDelay(Duration.ZERO);
			t.setShowDuration(Duration.INDEFINITE);
			
			Tooltip.install(s, t);
			
			// salva per picking
			s.setUserData(n);
			
			neuronSpheres.add(s);
			root3d.getChildren().add(s);
		}
	}

	private Color getLayerColor(int layerId) {
		Color startColor = Color.GREEN;
		Color endColor = Color.BROWN;

		double p = 1.0 / (6-layerId);
		double r = startColor.getRed() * p + endColor.getRed() * (1 - p);
		double g = startColor.getGreen() * p + endColor.getGreen() * (1 - p);
		double b = startColor.getBlue() * p + endColor.getBlue() * (1 - p);
		return Color.color(r,g,b);
	}

	private Node makeConnection(Point3D a, Point3D b, Color color) {

		Point3D diff = b.subtract(a);
		double length = diff.magnitude();

		Cylinder line = new Cylinder(0.002, length);
		line.setMaterial(new PhongMaterial(color));

		// midpoint
		Point3D mid = a.midpoint(b);
		line.setTranslateX(mid.getX());
		line.setTranslateY(mid.getY());
		line.setTranslateZ(mid.getZ());

		// orient cylinder
		Point3D yAxis = new Point3D(0, 1, 0);
		Point3D axis = diff.normalize();
		double angle = Math.acos(yAxis.dotProduct(axis));
		Point3D rotAxis = yAxis.crossProduct(axis);

		if (!rotAxis.equals(Point3D.ZERO)) {
			line.getTransforms().add(new Rotate(Math.toDegrees(angle), rotAxis));
		}
		return line;
	}

}
