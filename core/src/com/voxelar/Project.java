package com.voxelar;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.OrderedMap;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class Project {
    static final String BIN = ".bin";
    static final String JSON = ".json";

    String name;
    String tmp_name;

    FileHandle file_bin;
    FileHandle file_data;

    Project(String name) {
        this.name = name;
        file_bin = Gdx.files.external(name + BIN);
        file_data = Gdx.files.external(name + JSON);
    }

    boolean load(World world, Camera camera) {
        if (file_bin.exists()) {
            world.pointer = 0;
            ByteBuffer buffer = ByteBuffer.wrap(file_bin.readBytes());
            while (buffer.hasRemaining()) {
                world.vertices[world.pointer++] = buffer.getFloat();
            }

            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(file_data);
            JsonValue cameraPosition = root.get("camera").get("position");
            camera.position.set(cameraPosition.getFloat("x"), cameraPosition.getFloat("y"),  cameraPosition.getFloat("z"));
            JsonValue cameraUp = root.get("camera").get("up");
            camera.up.set(cameraUp.getFloat("x"), cameraUp.getFloat("y"), cameraUp.getFloat("z"));
            JsonValue center = root.get("camera").get("direction");
            camera.direction.set(center.getFloat("x"), center.getFloat("y"), center.getFloat("z"));

            return true;
        }
        return false;
    }

    void save(World world, Camera camera) {
        ByteBuffer buffer = ByteBuffer.allocate(world.pointer * Float.BYTES);
        for (int i = 0; i < world.pointer; i++) {
            buffer.putFloat(world.vertices[i]);
        }
        file_bin.writeBytes(buffer.array(), false);

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setWriter(new JsonWriter(new StringWriter()));
        json.writeObjectStart();
        {
            json.writeObjectStart("camera");
            {
                json.writeObjectStart("position");
                json.writeValue("x", camera.position.x);
                json.writeValue("y", camera.position.y);
                json.writeValue("z", camera.position.z);
                json.writeObjectEnd();
            }
            {
                json.writeObjectStart("up");
                json.writeValue("x", camera.up.x);
                json.writeValue("y", camera.up.y);
                json.writeValue("z", camera.up.z);
                json.writeObjectEnd();
            }
            {
                json.writeObjectStart("direction");
                json.writeValue("x", camera.direction.x);
                json.writeValue("y", camera.direction.y);
                json.writeValue("z", camera.direction.z);
                json.writeObjectEnd();
            }
            json.writeObjectEnd();
        }

        json.writeObjectEnd();
        file_data.writeString(json.getWriter().getWriter().toString(),false);
    }
}
