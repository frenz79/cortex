package com.cortex.viewer;

import com.cortex.commons.Point3f;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

public class Line3D extends Group {

    public Line3D(Point3f start, Point3f end, Color color, double radius) {

        Point3D p1 = new Point3D(start.x(), start.y(), start.z());
        Point3D p2 = new Point3D(end.x(), end.y(), end.z());

        Point3D diff = p2.subtract(p1);
        double height = diff.magnitude();

        Cylinder line = new Cylinder(radius, height);
        line.setMaterial(new PhongMaterial(color));
        line.setCullFace(CullFace.NONE);

        // midpoint
        Point3D mid = p1.midpoint(p2);
        line.setTranslateX(mid.getX());
        line.setTranslateY(mid.getY());
        line.setTranslateZ(mid.getZ());

        // orient cylinder
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D axisOfRotation = diff.crossProduct(yAxis);
        double angle = Math.acos(diff.normalize().dotProduct(yAxis));
        angle = Math.toDegrees(angle);

        if (!axisOfRotation.equals(Point3D.ZERO)) {
            line.getTransforms().add(new Rotate(angle, axisOfRotation));
        }

        getChildren().add(line);
    }
}