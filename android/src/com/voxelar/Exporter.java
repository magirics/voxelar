package com.voxelar;

import android.util.Log;

import com.badlogic.gdx.math.Vector3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Exporter {
    static FloatBuffer getData(File project) {
        byte[] raw = new byte[(int) project.length()];

        try {
            FileInputStream reader = new FileInputStream(project);
            reader.read(raw);
            reader.close();
        } catch (Exception ignored) {
        }

        return ByteBuffer.wrap(raw).asFloatBuffer();
    }

    static void toSTL(File source, FileOutputStream writer, float scale) {
        int numVoxels = Voxel.numVoxels((int) source.length());
        int numTriangles = numVoxels * Voxel.TRIANGLES;
        float s = 10 * scale;

        FloatBuffer sourceData = getData(source);

        ByteBuffer data = ByteBuffer.allocate(84 + numVoxels * Voxel.TRIANGLES * 50);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.position(80);
        data.putInt(numTriangles);
        for (int i = Voxel.OFF; i < sourceData.capacity(); i += Voxel.STEP) {
            float x = sourceData.get(i),
                    z = sourceData.get(i+1),
                    y = sourceData.get(i+2);

            x *= s;
            y *= s;
            z *= s;

            //Left
            data.putFloat(-1).putFloat(0).putFloat(0);
            data.putFloat(x).putFloat(y + s).putFloat(z);
            data.putFloat(x).putFloat(y).putFloat(z);
            data.putFloat(x).putFloat(y).putFloat(z + s);
            data.putShort((short) 0);

            data.putFloat(-1).putFloat(0).putFloat(0);
            data.putFloat(x).putFloat(y + s).putFloat(z);
            data.putFloat(x).putFloat(y).putFloat(z + s);
            data.putFloat(x).putFloat(y + s).putFloat(z + s);
            data.putShort((short) 0);

            //Top
            data.putFloat(0).putFloat(1).putFloat(0);
            data.putFloat(x).putFloat(y + s).putFloat(z);
            data.putFloat(x).putFloat(y + s).putFloat(z + s);
            data.putFloat(x + s).putFloat(y + s).putFloat(z + s);
            data.putShort((short) 0);

            data.putFloat(0).putFloat(1).putFloat(0);
            data.putFloat(x).putFloat(y + s).putFloat(z);
            data.putFloat(x + s).putFloat(y + s).putFloat(z + s);
            data.putFloat(x + s).putFloat(y + s).putFloat(z);
            data.putShort((short) 0);

            //Right
            data.putFloat(1).putFloat(0).putFloat(0);
            data.putFloat(x + s).putFloat(y + s).putFloat(z + s);
            data.putFloat(x + s).putFloat(y).putFloat(z + s);
            data.putFloat(x + s).putFloat(y).putFloat(z);
            data.putShort((short) 0);

            data.putFloat(1).putFloat(0).putFloat(0);
            data.putFloat(x + s).putFloat(y + s).putFloat(z + s);
            data.putFloat(x + s).putFloat(y).putFloat(z);
            data.putFloat(x + s).putFloat(y + s).putFloat(z);
            data.putShort((short) 0);

            //Bottom
            data.putFloat(0).putFloat(-1).putFloat(0);
            data.putFloat(x).putFloat(y).putFloat(z + s);
            data.putFloat(x).putFloat(y).putFloat(z);
            data.putFloat(x + s).putFloat(y).putFloat(z);
            data.putShort((short) 0);

            data.putFloat(0).putFloat(-1).putFloat(0);
            data.putFloat(x).putFloat(y).putFloat(z + s);
            data.putFloat(x + s).putFloat(y).putFloat(z);
            data.putFloat(x + s).putFloat(y).putFloat(z + s);
            data.putShort((short) 0);

            //Front
            data.putFloat(0).putFloat(0).putFloat(1);
            data.putFloat(x).putFloat(y + s).putFloat(z + s);
            data.putFloat(x).putFloat(y).putFloat(z + s);
            data.putFloat(x + s).putFloat(y).putFloat(z + s);
            data.putShort((short) 0);

            data.putFloat(0).putFloat(0).putFloat(1);
            data.putFloat(x).putFloat(y + s).putFloat(z + s);
            data.putFloat(x + s).putFloat(y).putFloat(z + s);
            data.putFloat(x + s).putFloat(y + s).putFloat(z + s);
            data.putShort((short) 0);

            //Back
            data.putFloat(0).putFloat(0).putFloat(-1);
            data.putFloat(x + s).putFloat(y + s).putFloat(z);
            data.putFloat(x + s).putFloat(y).putFloat(z);
            data.putFloat(x).putFloat(y).putFloat(z);
            data.putShort((short) 0);

            data.putFloat(0).putFloat(0).putFloat(-1);
            data.putFloat(x + s).putFloat(y + s).putFloat(z);
            data.putFloat(x).putFloat(y).putFloat(z);
            data.putFloat(x).putFloat(y + s).putFloat(z);
            data.putShort((short) 0);
        }

        try {
            writer.write(data.array());
        } catch (Exception ignored) {
        }
    }
}
