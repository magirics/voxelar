package com.voxelar;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import javax.print.DocFlavor;

public class World implements RenderableProvider {
    static final float VOXEL_RADIUS = 0.5f;

    static final int WIDTH = 20;
    static final int HEIGHT = 20;
    static final int DEPTH = 20;

    static final int MAX_VOXELS = 2730;

    static final int FACES_PER_VOXEL = 6;
    static final int VERTICES_PER_VOXEL = 4 * FACES_PER_VOXEL;
    static final int INDICES_PER_VOXEL = 6 * FACES_PER_VOXEL;

    static final int MAX_FACES = MAX_VOXELS * FACES_PER_VOXEL;
    static final int MAX_VERTICES = MAX_VOXELS * VERTICES_PER_VOXEL;
    static final int MAX_INDICES = MAX_VOXELS * INDICES_PER_VOXEL;

    static final int FLOATS_PER_VERTEX = 3 + 3 + 4; // px,py,pz + nx,ny,nz + r,g,b,a;
    static final int FLOATS_PER_VOXEL = VERTICES_PER_VOXEL * FLOATS_PER_VERTEX;

    int off = (4 + 1)*FLOATS_PER_VERTEX;
    int step = FLOATS_PER_VOXEL;

    Renderable renderable;
    Mesh mesh;
    float[] vertices;
    short[] indices;
    int pointer;

    BoundingBox box = new BoundingBox();
    Vector3 minimum = new Vector3(), maximum = new Vector3();
    Vector3 collisionPoint = new Vector3(), normalFace = new Vector3();
    Vector3 voxelCollided = new Vector3();

    //Lines
    int pointPerFace = 4*2;
    int linePointer;
    short[] lineIndices;
    Renderable renderableline;
    Mesh meshLine;

    World() {
        this.vertices = new float[MAX_VERTICES * FLOATS_PER_VERTEX];
        this.pointer = 0;
        this.indices = new short[MAX_INDICES];

        int j = 0;
        for (int i = 0; i < indices.length; i += 6, j += 4) {
            indices[i + 0] = (short) (j + 0);
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 2);
            indices[i + 3] = (short) (j + 0);
            indices[i + 4] = (short) (j + 2);
            indices[i + 5] = (short) (j + 3);
        }

        mesh = new Mesh(false, MAX_VERTICES, MAX_INDICES,
                VertexAttribute.Position(),
                VertexAttribute.Normal(),
                VertexAttribute.ColorUnpacked());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        renderable = new Renderable();
        renderable.material = new Material();
        renderable.meshPart.mesh = mesh;
        renderable.meshPart.primitiveType = GL20.GL_TRIANGLES;
        renderable.meshPart.offset = 0;


        //Lines
        linePointer = 0;
        lineIndices = new short[pointPerFace * MAX_FACES];

        for (int i = 0; linePointer < lineIndices.length; i+=4) {
            lineIndices[linePointer++] = (short) i;
            lineIndices[linePointer++] = (short) (i+1);

            lineIndices[linePointer++] = (short) (i+1);
            lineIndices[linePointer++] = (short) (i+2);

            lineIndices[linePointer++] = (short) (i+2);
            lineIndices[linePointer++] = (short) (i+3);

            lineIndices[linePointer++] = (short) i;
            lineIndices[linePointer++] = (short) (i+3);
        }

        //line
        meshLine = new Mesh(false, MAX_VERTICES, lineIndices.length,
                VertexAttribute.Position(),
                new VertexAttribute(VertexAttributes.Usage.Generic, 3, "filling1"),
                new VertexAttribute(VertexAttributes.Usage.Generic, 4, "filling2"));

        meshLine.setVertices(vertices);
        meshLine.setIndices(lineIndices);

        renderableline = new Renderable();
        renderableline.material = new Material();
        renderableline.meshPart.mesh = meshLine;
        renderableline.meshPart.primitiveType = GL20.GL_LINES;
        renderableline.meshPart.offset = 0;
    }

    @Override
    public void getRenderables(Array<Renderable> renderables, Pool<Renderable> pool) {
        mesh.updateVertices(0, vertices);
        renderable.meshPart.size = pointer / FLOATS_PER_VERTEX / 4 * 6;

        meshLine.updateVertices(0, vertices);
        renderableline.meshPart.size = pointer / FLOATS_PER_VERTEX / 4 * pointPerFace;

        renderables.add(renderable, renderableline);
    }

    void add(Ray ray, Color color) {
        Vector3[] data = collide(ray);
        if (data != null) {
            Vector3 position = data[0].add(data[1]);
            set((int) position.x, (int) position.y, (int) position.z, color);
        }
    }

    void del(int x, int y, int z) {
        int pos;
        for (pos = off; pos < vertices.length; pos += step) {
            if (x == vertices[pos] && y == vertices[pos + 1] && z == vertices[pos + 2])
                break;
        }

        pos -= off;
        pointer -= step;
        for (int i = 0; i < step; i++) {
            vertices[pos + i] = vertices[pointer + i];
        }
    }

    void del(Ray ray) {
        Vector3[] data = collide(ray);
        if (data != null)
            del((int) data[0].x, (int) data[0].y, (int) data[0].z);
    }

    void set(int x, int y, int z, Color c) {
        createTop(x, y, z, c);
        createBottom(x, y, z, c);
        createFront(x, y, z, c);
        createBack(x, y, z, c);
        createRight(x, y, z, c);
        createLeft(x, y, z, c);
    }

    Vector3[] collide(Ray ray) {
        float shortestDistance = Float.MAX_VALUE;
        boolean collided = false;

        for (int i = off; i < pointer; i += step) {
            float x = vertices[i + 0], y = vertices[i + 1], z = vertices[i + 2];
            box.set(minimum.set(x, y, z), maximum.set(x + 1, y + 1, z + 1));
            float distance = minimum.dst2(ray.origin);

            if (distance > 400) continue; // 20**2

            if (distance < shortestDistance && Intersector.intersectRayBoundsFast(ray, box)) {
                collided = Intersector.intersectRayBounds(ray, box, collisionPoint);
                voxelCollided.set(minimum);
                shortestDistance = distance;
            }
        }

        if (collided) {
            normalFace.set(collisionPoint).sub(0.5f).sub(voxelCollided);

            normalFace.x = Math.round(normalFace.x * 1000) / 1000f;
            normalFace.y = Math.round(normalFace.y * 1000) / 1000f;
            normalFace.z = Math.round(normalFace.z * 1000) / 1000f;

            if (Math.abs(normalFace.x) == VOXEL_RADIUS)
                normalFace.set(normalFace.x, 0, 0);
            else if (Math.abs(normalFace.y) == VOXEL_RADIUS)
                normalFace.set(0, normalFace.y, 0);
            else
                normalFace.set(0, 0, normalFace.z);

            return new Vector3[]{voxelCollided, normalFace.scl(2)};
        }

        return null;
    }

    void createTop(int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;
    }

    void createBottom(int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;
    }

    void createFront(int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;
    }

    void createBack(int x, int y, int z, Color c) {
        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;
    }


    void createRight(int x, int y, int z, Color c) {
        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;
    }

    void createLeft(int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;
    }
}

class Voxel {
    static final int FACES = 6;
    static final int TRIANGLES = FACES * 2;
    static final int VERTICES = FACES * 4;
    static final int INDICES = FACES * 6;

    static final int FLOATS_PER_VERTEX = 3 + 3 + 4; // px,py,pz + nx,ny,nz + r,g,b,a;
    static final int FLOATS_PER_TRIANGLE = FLOATS_PER_VERTEX * 3;
    static final int FLOATS_PER_FACE = FLOATS_PER_VERTEX * 4;
    static final int FLOATS = FLOATS_PER_FACE * FACES;

    static final int OFF = (4 + 1) * FLOATS_PER_VERTEX;
    static final int STEP = FLOATS;

    static int numVoxels(int numBytes) {
        return (numBytes / Float.BYTES) / FLOATS;
    }

    static int numBytes(int numVoxels) {
        return (numVoxels * FLOATS) * Float.BYTES;
    }

    int createAll(float[] vertices, int pointer, int x, int y, int z, Color c) {
        pointer = createTop(vertices, pointer, x, y, z, c);
        pointer = createBottom(vertices, pointer, x, y, z, c);
        pointer = createFront(vertices, pointer, x, y, z, c);
        pointer = createBack(vertices, pointer, x, y, z, c);
        pointer = createRight(vertices, pointer, x, y, z, c);
        pointer = createLeft(vertices, pointer, x, y, z, c);

        return pointer;
    }

    int createTop(float[] vertices, int pointer, int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        return pointer;
    }

    int createBottom(float[] vertices, int pointer, int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        return pointer;
    }

    int createFront(float[] vertices, int pointer, int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        return pointer;
    }

    int createBack(float[] vertices, int pointer, int x, int y, int z, Color c) {
        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        return pointer;
    }


    int createRight(float[] vertices, int pointer, int x, int y, int z, Color c) {
        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x + 1;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        return pointer;
    }

    int createLeft(float[] vertices, int pointer, int x, int y, int z, Color c) {
        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        vertices[pointer++] = x;
        vertices[pointer++] = y + 1;
        vertices[pointer++] = z + 1;
        vertices[pointer++] = 0;
        vertices[pointer++] = -1;
        vertices[pointer++] = 0;
        vertices[pointer++] = c.r;
        vertices[pointer++] = c.g;
        vertices[pointer++] = c.b;
        vertices[pointer++] = c.a;

        return pointer;
    }
}
