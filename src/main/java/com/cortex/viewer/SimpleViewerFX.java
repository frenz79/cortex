package com.cortex.viewer;

import com.cortex.brain.Brain;
import com.cortex.brain.layers.Layer;
import com.cortex.commons.Point3f;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

public class SimpleViewerFX extends Application {

    private static Brain brain;
    private static boolean drawNeurons;
    private static boolean drawSynapses;

    private double mouseOldX, mouseOldY;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate translate = new Translate(0, 0, -500);

    public static void launchViewer(Brain b, boolean dn, boolean ds) {
        brain = b;
        drawNeurons = dn;
        drawSynapses = ds;
        launch();
    }

    @Override
    public void start(Stage stage) {
        Group root3d = new Group();
        root3d.getTransforms().addAll(rotateX, rotateY, translate);

        if (drawNeurons) {
            addNeuronLayers(root3d);
        }

        // (opzionale) sinapsi
        // if (drawSynapses) addSynapses(root3d);

        SubScene subScene = new SubScene(root3d, 1200, 800, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        subScene.setCamera(camera);

        BorderPane pane = new BorderPane(subScene);
        Scene scene = new Scene(pane);

        enableMouseControls(scene, root3d);

        stage.setTitle("SimpleViewerFX");
        stage.setScene(scene);
        stage.show();
    }

    private void addNeuronLayers(Group root) {
        int numLayers = brain.getAllLayers().size();

        for (Layer layer : brain.getAllLayers()) {
            Color color = getGradient(
                    layer.getLayerId(),
                    numLayers,
                    Color.RED,
                    Color.CYAN
            );

            Group layerGroup = new Group();

            brain.streamAllNeurons(n -> {
                Point3f p = n.getPosition();
                Sphere s = new Sphere(1.5);
                s.setTranslateX(p.x());
                s.setTranslateY(p.y());
                s.setTranslateZ(p.z());

                PhongMaterial mat = new PhongMaterial(color);
                s.setMaterial(mat);

                layerGroup.getChildren().add(s);
                return true;
            });

            root.getChildren().add(layerGroup);
        }
    }

    private Color getGradient(int id, int steps, Color start, Color end) {
        double ratio = (double) id / (double) steps;
        return start.interpolate(end, ratio);
    }

    private void enableMouseControls(Scene scene, Group root) {

        scene.setOnMousePressed(e -> {
            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
        });

        scene.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - mouseOldX;
            double dy = e.getSceneY() - mouseOldY;

            if (e.getButton() == MouseButton.PRIMARY) {
                rotateY.setAngle(rotateY.getAngle() + dx * 0.3);
                rotateX.setAngle(rotateX.getAngle() - dy * 0.3);
            }
            if (e.getButton() == MouseButton.SECONDARY) {
                translate.setX(translate.getX() + dx * 0.5);
                translate.setY(translate.getY() + dy * 0.5);
            }

            mouseOldX = e.getSceneX();
            mouseOldY = e.getSceneY();
        });

        scene.addEventFilter(ScrollEvent.SCROLL, e -> {
            translate.setZ(translate.getZ() + e.getDeltaY() * 0.5);
        });
    }
}