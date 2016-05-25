package com.boobastudio.geophotogallery;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.WebView;

public class ItemViewActivity extends AppCompatActivity {
    WebView mWebView;
    final static int NO_REPEAT = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        setContentView(R.layout.single_photo);
        mWebView = (WebView) findViewById(R.id.single_photo_web_view);

        String url = getIntent().getStringExtra("url");
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.loadUrl(url);

        mWebView.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);
        applyRotation(0, 0, 360);

        Log.d("ItemViewActivity", "url_l is "+ url);
    }
    //animation to rotate around x axis
    private void applyRotation(int position, float start, float end) {

        final float centerX = mWebView.getWidth() / 2.0f;
        final float centerY = mWebView.getHeight() / 2.0f;

        final Rotate3dAnimation rotation =
                new Rotate3dAnimation(start, end,  centerX + 480f, centerY + 400f, 0.0f, true);
        rotation.setDuration(500);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.setRepeatCount(NO_REPEAT);

        mWebView.startAnimation(rotation);
    }

}
