package com.voxelar;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class Controller implements GestureDetector.GestureListener {
    Main main;
    PerspectiveCamera camera;
    World world;

    Vector3 center = new Vector3();
    Vector3 tempVec2 = new Vector3();
    Vector3 tempVec3 = new Vector3();

    float dragFactor = 0.2f;
    float zoomFactor = 20;

    Vector3 tempVec0 = new Vector3();
    float previousZoom;
    Vector3 tempVec1 = new Vector3();

    //Center
    Vector2 cent = new Vector2(Gdx.graphics.getWidth()/2.0f, Gdx.graphics.getHeight()/2.0f);
    Vector2 tapTouch = new Vector2();

    Controller(Main main) {
        this.main = main;
        this.camera = main.camera;
        this.world = main.world;
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        previousZoom = 0;
        tapTouch.set(x, y);

        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        Ray ray = camera.getPickRay(x, y);
        if (main.getToolsStatus("delete"))
            world.del(ray);
        else
            world.add(ray, main.getSelectedColor());


        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        Ray ray = camera.getPickRay(x, y);
        world.del(ray);
        Gdx.input.vibrate(50);

        return true;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        tempVec0.set(camera.direction).crs(camera.up);
        center.set(camera.direction).setLength(15).add(camera.position);

        if (cent.dst(tapTouch) < main.cs * 1.1f) {
            //camera.rotate(camera.up, dragFactor * deltaX);
            //camera.rotate(tempVec0, dragFactor * deltaY);

            camera.direction.rotate(camera.up, -dragFactor * deltaX);
            camera.direction.rotate(tempVec0, -dragFactor * deltaY);
        } else {
            //camera.rotateAround(center, camera.up, dragFactor * deltaX);
            //camera.rotateAround(center, tempVec0, dragFactor * deltaY);

            camera.position.sub(center);
            camera.position.rotate(camera.up, -dragFactor * deltaX);
            camera.position.rotate(tempVec0, -dragFactor * deltaY);
            camera.position.add(center);
            camera.lookAt(center);
        }
        //camera.up.set(Vector3.Y);

        return true;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        float newZoom = distance - initialDistance;
        float amount = newZoom - previousZoom;
        previousZoom = newZoom;

        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        amount = zoomFactor * (amount / Math.min(w, h));

        camera.translate(tempVec1.set(camera.direction).scl(amount));
        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }
}
