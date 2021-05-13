package com.voxelar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.ImageViewCompat;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;

public class CreateActivity extends AndroidApplication {

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create);

		ConstraintLayout ground = findViewById(R.id.ground);
		final ToggleButton toolDelete = findViewById(R.id.tool_delete);
		AppCompatImageView colorBar = findViewById(R.id.color_bar);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.numSamples = 2;

		String name = getIntent().getExtras().getString("name");
		final Main main = new Main();
		main.setProject(name);
		View view = initializeForView(main, config);
		ground.addView(view, 0);

		colorBar.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				ImageView imageView = ((ImageView)v);
				Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
				int x = (int)event.getX();
				x = x * bitmap.getWidth() / v.getWidth();

				int pixel = bitmap.getPixel(x, 0);
				Color color = new Color();
				Color.rgb888ToColor(color, pixel);
				main.setSelectedColor(color);

				main.setToolStatus("delete", false);
				toolDelete.setChecked(false);

				return true;
			}
		});

		main.setToolStatus("delete", false);
		toolDelete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				main.setToolStatus("delete", isChecked);
			}
		});
	}
}