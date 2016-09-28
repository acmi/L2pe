/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.l2pe.view;

import javafx.scene.Group;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

public class Xform extends Group {

    public enum RotateOrder {
        XYZ, XZY, YXZ, YZX, ZXY, ZYX
    }

    public Translate t = new Translate();
    public Translate p = new Translate();
    public Translate ip = new Translate();
    public Rotate rx = new Rotate();

    {
        rx.setAxis(Rotate.X_AXIS);
    }

    public Rotate ry = new Rotate();

    {
        ry.setAxis(Rotate.Y_AXIS);
    }

    public Rotate rz = new Rotate();

    {
        rz.setAxis(Rotate.Z_AXIS);
    }

    public Scale s = new Scale();

    public Xform() {
        super();
        getTransforms().addAll(t, rz, ry, rx, s);
    }

    public Xform(RotateOrder rotateOrder) {
        super();
        // choose the order of rotations based on the rotateOrder
        switch (rotateOrder) {
            case XYZ:
                getTransforms().addAll(t, p, rz, ry, rx, s, ip);
                break;
            case XZY:
                getTransforms().addAll(t, p, ry, rz, rx, s, ip);
                break;
            case YXZ:
                getTransforms().addAll(t, p, rz, rx, ry, s, ip);
                break;
            case YZX:
                getTransforms().addAll(t, p, rx, rz, ry, s, ip);  // For Camera
                break;
            case ZXY:
                getTransforms().addAll(t, p, ry, rx, rz, s, ip);
                break;
            case ZYX:
                getTransforms().addAll(t, p, rx, ry, rz, s, ip);
                break;
        }
    }

    public void setTranslate(double x, double y, double z) {
        t.setX(x);
        t.setY(y);
        t.setZ(z);
    }

    public void setTranslate(double x, double y) {
        t.setX(x);
        t.setY(y);
    }

    // Cannot override these methods as they are final:
    // public void setTranslateX(double x) { t.setX(x); }
    // public void setTranslateY(double y) { t.setY(y); }
    // public void setTranslateZ(double z) { t.setZ(z); }
    // Use these methods instead:
    public void setTx(double x) {
        t.setX(x);
    }

    public void setTy(double y) {
        t.setY(y);
    }

    public void setTz(double z) {
        t.setZ(z);
    }

    public void setRotate(double x, double y, double z) {
        rx.setAngle(x);
        ry.setAngle(y);
        rz.setAngle(z);
    }

    public void setRotateX(double x) {
        rx.setAngle(x);
    }

    public void setRotateY(double y) {
        ry.setAngle(y);
    }

    public void setRotateZ(double z) {
        rz.setAngle(z);
    }

    public void setRx(double x) {
        rx.setAngle(x);
    }

    public void setRy(double y) {
        ry.setAngle(y);
    }

    public void setRz(double z) {
        rz.setAngle(z);
    }

    public void setScale(double scaleFactor) {
        s.setX(scaleFactor);
        s.setY(scaleFactor);
        s.setZ(scaleFactor);
    }

    public void setScale(double x, double y, double z) {
        s.setX(x);
        s.setY(y);
        s.setZ(z);
    }

    // Cannot override these methods as they are final:
    // public void setScaleX(double x) { s.setX(x); }
    // public void setScaleY(double y) { s.setY(y); }
    // public void setScaleZ(double z) { s.setZ(z); }
    // Use these methods instead:
    public void setSx(double x) {
        s.setX(x);
    }

    public void setSy(double y) {
        s.setY(y);
    }

    public void setSz(double z) {
        s.setZ(z);
    }

    public void setPivot(double x, double y, double z) {
        p.setX(x);
        p.setY(y);
        p.setZ(z);
        ip.setX(-x);
        ip.setY(-y);
        ip.setZ(-z);
    }

    public void reset() {
        t.setX(0.0);
        t.setY(0.0);
        t.setZ(0.0);
        rx.setAngle(0.0);
        ry.setAngle(0.0);
        rz.setAngle(0.0);
        s.setX(1.0);
        s.setY(1.0);
        s.setZ(1.0);
        p.setX(0.0);
        p.setY(0.0);
        p.setZ(0.0);
        ip.setX(0.0);
        ip.setY(0.0);
        ip.setZ(0.0);
    }

    public void resetTSP() {
        t.setX(0.0);
        t.setY(0.0);
        t.setZ(0.0);
        s.setX(1.0);
        s.setY(1.0);
        s.setZ(1.0);
        p.setX(0.0);
        p.setY(0.0);
        p.setZ(0.0);
        ip.setX(0.0);
        ip.setY(0.0);
        ip.setZ(0.0);
    }

    @Override
    public String toString() {
        return "Xform[t = (" +
                t.getX() + ", " +
                t.getY() + ", " +
                t.getZ() + ")  " +
                "r = (" +
                rx.getAngle() + ", " +
                ry.getAngle() + ", " +
                rz.getAngle() + ")  " +
                "s = (" +
                s.getX() + ", " +
                s.getY() + ", " +
                s.getZ() + ")  " +
                "p = (" +
                p.getX() + ", " +
                p.getY() + ", " +
                p.getZ() + ")  " +
                "ip = (" +
                ip.getX() + ", " +
                ip.getY() + ", " +
                ip.getZ() + ")]";
    }
}
