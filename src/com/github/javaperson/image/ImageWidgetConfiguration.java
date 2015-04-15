package com.github.javaperson.image;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by wangyang on 15/4/9.
 */
public class ImageWidgetConfiguration extends Activity {
    public static final String PREFS_NAME = "com.github.javaperson.image.preferences";
    private int id;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configuration);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            id = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

    }

    public void addWidget(View v) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        String url = ((TextView) findViewById(R.id.url)).getText().toString();
        editor.putString(String.valueOf(id), url);
        editor.commit();
        Log.d("Configuration", url);

        setResult(RESULT_OK,
                new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        );
        finish();
    }
}
