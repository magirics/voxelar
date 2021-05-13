package com.voxelar;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;

public class Main extends ApplicationAdapter {
	Color selectedColor = Color.GRAY;
	HashMap<String, Boolean> toolsStatus = new HashMap<>();

	SpriteBatch spriteBatch;
	BitmapFont font;

	PerspectiveCamera camera;

	ModelBatch batch;
	World world;

	Project project;
	String projectName;

	Texture center;
	int cs = 36;
	@Override
	public void create () {
		cs = (int) (0.05 * Gdx.graphics.getWidth());
		center = new Texture("center.png");

		font = new BitmapFont();
		font.getData().setScale(2f);
		font.setColor(Color.BLACK);
		spriteBatch = new SpriteBatch();

		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.far = 100;
		camera.near = 1;
		camera.position.set(5, 5, 5);
		camera.lookAt(Vector3.Zero);

		batch = new ModelBatch();

		world = new World();
		project = new Project(projectName);
		if (!project.load(world, camera)) {
			camera.up.set(Vector3.Y);
			camera.position.set(10, 10, 10);

			world.pointer = 0;

			world.set(0,0,0, Color.GRAY);

			/*world.set(1, 0, 0, Color.GRAY);
			world.set(0, 0, 1, Color.GRAY);
			world.set(1, 0, 2, Color.GRAY);
			world.set(2, 0, 1, Color.GRAY);

			world.set(1, 0, 1, Color.GRAY);
			world.set(1, 1, 1, Color.GRAY);*/
		}

		Gdx.input.setInputProcessor(new GestureDetector(new Controller(this)));
	}

	@Override
	public void render () {
		//Gdx.gl.glLineWidth(3);
		Gdx.gl.glClearColor(1, 1, 1, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT |
				GL20.GL_DEPTH_BUFFER_BIT |
				(Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

		camera.up.interpolate(Vector3.Y, 0.4f, Interpolation.linear);
		camera.update();

		batch.begin(camera);
		batch.render(world);
		batch.end();

		spriteBatch.begin();
		spriteBatch.draw(center, Gdx.graphics.getWidth()/2f - cs/2f,Gdx.graphics.getHeight()/2f - cs/2f,cs,cs);
		font.draw(spriteBatch, "fps: " + Gdx.graphics.getFramesPerSecond(), 50, Gdx.graphics.getHeight() - 50);
		spriteBatch.end();
	}

	@Override
	public void pause() {
		Gdx.gl.glClearColor(1, 1, 1, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT |
				GL20.GL_DEPTH_BUFFER_BIT |
				(Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
		batch.begin(camera);
		batch.render(world);
		batch.end();

		int x = Gdx.graphics.getWidth(), y = Gdx.graphics.getHeight();
		int s = (int)(x * 0.75f);
		Pixmap invertedPixmap = Pixmap.createFromFrameBuffer(x / 2 - s / 2, y / 2 - s / 2, s, s);
		Pixmap normalPixmap = new Pixmap(s, s, Pixmap.Format.RGBA8888);
		for (int j = 0; j < s; j++) {
			for (int i = 0; i < s; i++) {
				int color = invertedPixmap.getPixel(i, s - j);
				normalPixmap.drawPixel(i, j, color);
			}
		}
		invertedPixmap.dispose();
		project.save(world,camera);
		FileHandle file = Gdx.files.external(projectName + ".png");
		PixmapIO.writePNG(file, normalPixmap);
		normalPixmap.dispose();
	}

	@Override
	public void dispose () {
		center.dispose();
		spriteBatch.dispose();
		batch.dispose();
		project.save(world, camera);
	}

	@Override
	public void resume() {
		super.resume();
	}

	Color getSelectedColor() {
		return selectedColor;
	}

	void setSelectedColor(Color color) {
		this.selectedColor = color;
	}

	boolean getToolsStatus(String tool) {
		return toolsStatus.get(tool);
	}

	void setToolStatus(String tool, boolean status) {
		toolsStatus.put(tool, status);
	}

	void setProject(String name) {
		this.projectName = name;
	}
}