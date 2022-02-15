package org.volumelighting.vl;

import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class FrustumVolumeMesh extends Mesh {

    private int resolution;
    private Camera targetCam;

    private List<Vector3f> finalVertices = new ArrayList<>();
    private List<Integer> finalFaces = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param resolution
     * @param targetCam
     */
    public FrustumVolumeMesh(int resolution, Camera targetCam) {
        this.targetCam = targetCam;
        this.resolution = resolution;
        generateMesh();
    }

    public float farPlaneGridDistance() {
        return targetCam.getFrustumFar();
    }

    private void generateMesh() {

        float stepX = 2f * (targetCam.getFrustumLeft() / targetCam.getFrustumNear()) / resolution;
        float stepY = 2f * (targetCam.getFrustumTop() / targetCam.getFrustumNear()) / resolution;

        // stepX = stepY = step;
        // offsetX = offsetY = offset;
        float x, y;
        float z = targetCam.getFrustumNear();

        stepX *= z;
        stepY *= z;

        float offsetX = -stepX * resolution / 2.0f;
        float offsetY = -stepY * resolution / 2.0f;

        finalVertices.add(new Vector3f(0, 0, z));

        // top ring
        for (int ii = 0; ii < resolution; ii++) {
            y = (float) ii * stepY + offsetY;
            finalVertices.add(new Vector3f(offsetX, y, z));
        }

        for (int ii = 1; ii < resolution - 1; ii++) {
            x = (float) ii * stepX + offsetX;
            finalVertices.add(new Vector3f(x, offsetY, z));
            finalVertices.add(new Vector3f(x, -1f * offsetY - stepY, z));
        }

        x = -1f * offsetX - stepX;
        for (int ii = 0; ii < resolution; ii++) {
            y = (float) ii * stepY + offsetY;
            finalVertices.add(new Vector3f(x, y, z));
        }

        stepX /= z;
        stepY /= z;

        // front grid
        z = farPlaneGridDistance();
        stepX *= z;
        stepY *= z;
        offsetX = -stepX * resolution / 2.0f;
        offsetY = -stepY * resolution / 2.0f;

        for (int ii = 0; ii < resolution; ii++) {
            for (int jj = 0; jj < resolution; jj++) {
                x = ii * stepX + offsetX;
                y = jj * stepY + offsetY;
                finalVertices.add(new Vector3f(x, y, z));
            }
        }

        // faces
        for (int ii = 1; ii < resolution; ii++) {
            for (int jj = 1; jj < resolution; jj++) {

                finalFaces.add(calcIndex(jj - 1, ii - 1));
                finalFaces.add(calcIndex(jj, ii - 1));
                finalFaces.add(calcIndex(jj - 1, ii));

                finalFaces.add(calcIndex(jj, ii));
                finalFaces.add(calcIndex(jj - 1, ii));// swap this with the below if faces are on wrong side
                finalFaces.add(calcIndex(jj, ii - 1));
            }
        }

        // edges
        int p1, p2, p3, p4;
        for (int ii = 1; ii < resolution; ii++) {
            p1 = calcTopIndex(ii - 1, 0);
            p2 = calcTopIndex(ii, 0);
            p3 = calcIndex(ii - 1, 0);
            p4 = calcIndex(ii, 0);

            finalFaces.add(p1);
            finalFaces.add(p2);
            finalFaces.add(p3);

            finalFaces.add(p3);
            finalFaces.add(p2);
            finalFaces.add(p4);

            finalFaces.add(p1);
            finalFaces.add(0);
            finalFaces.add(p2);

            p1 = calcTopIndex(ii - 1, resolution - 1);
            p2 = calcTopIndex(ii, resolution - 1);
            p3 = calcIndex(ii - 1, resolution - 1);
            p4 = calcIndex(ii, resolution - 1);

            finalFaces.add(p1);
            finalFaces.add(p3);
            finalFaces.add(p2);

            finalFaces.add(p3);
            finalFaces.add(p4);
            finalFaces.add(p2);

            finalFaces.add(p1);
            finalFaces.add(p2);
            finalFaces.add(0);

            p1 = calcTopIndex(0, ii - 1);
            p2 = calcTopIndex(0, ii);
            p3 = calcIndex(0, ii - 1);
            p4 = calcIndex(0, ii);

            finalFaces.add(p1);
            finalFaces.add(p3);
            finalFaces.add(p2);

            finalFaces.add(p3);
            finalFaces.add(p4);
            finalFaces.add(p2);

            finalFaces.add(p1);
            finalFaces.add(p2);
            finalFaces.add(0);

            p1 = calcTopIndex(resolution - 1, ii - 1);
            p2 = calcTopIndex(resolution - 1, ii);
            p3 = calcIndex(resolution - 1, ii - 1);
            p4 = calcIndex(resolution - 1, ii);

            finalFaces.add(p1);
            finalFaces.add(p2);
            finalFaces.add(p3);

            finalFaces.add(p3);
            finalFaces.add(p2);
            finalFaces.add(p4);

            finalFaces.add(p1);

            finalFaces.add(0);
            finalFaces.add(p2);
        }

        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer((Vector3f[]) finalVertices.toArray(new Vector3f[finalVertices.size()])));
        setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(toIntArray(finalFaces)));

        updateBound();
        createCollisionData();

    }

    // calculates the index for a far plane face vert
    private int calcIndex(int row, int col) {
        int topVerts = (resolution - 1) * 4;
        return col * resolution + row + 1 + topVerts;
    }

    // calculates the index for near plane ring vert
    private int calcTopIndex(int row, int col) {
        int index = 1;
        if (col == 0) {
            return row + 1;
        }
        index += resolution;
        index += (col - 1) * 2;
        if (col == resolution - 1) {
            index += row;
        } else if (row == resolution - 1) {
            index += 1;
        }
        return index;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Integer ii : list) {
            ret[i++] = ii.intValue();
        }
        return ret;
    }

}
