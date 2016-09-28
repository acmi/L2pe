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
package acmi.l2.clientmod.l2pe.view.model;

public class Offsets implements Cloneable {
    public int mesh;
    public int meshSize;

    public int location;
    public int colLocation;
    public int basePos;

    public int drawScale;
    public int drawScale3D;

    public int rotation;
    public int swayRotationOrig;
    public int baseRot;

    public int rotationRate;

    public int zoneRenderState;
    public int zoneRenderStateCount;

    @Override
    public String toString() {
        return "Offsets{" +
                "mesh=0x" + Integer.toHexString(mesh) +
                ", meshSize=" + meshSize +
                ", location=0x" + Integer.toHexString(location) +
                ", rotation=0x" + Integer.toHexString(rotation) +
                ", swayRotationOrig=0x" + Integer.toHexString(swayRotationOrig) +
                ", colLocation=0x" + Integer.toHexString(colLocation) +
                ", basePos=0x" + Integer.toHexString(basePos) +
                ", baseRot=0x" + Integer.toHexString(baseRot) +
                ", drawScale=0x" + Integer.toHexString(drawScale) +
                ", rotationRate=0x" + Integer.toHexString(rotationRate) +
                ", zoneRenderState=0x" + Integer.toHexString(zoneRenderState) +
                ", zoneRenderStateCount=" + zoneRenderStateCount +
                '}';
    }

    @Override
    protected Offsets clone() {
        try {
            return (Offsets) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
