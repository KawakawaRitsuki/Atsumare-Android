package com.kawakawaplanning.atsumare.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import com.kawakawaplanning.atsumare.R;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SplashActivity extends Activity {

    @Bind(R.id.titleTv)
    TextView mTitleTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);


        mTitleTv.setTypeface(Typeface.createFromAsset(getAssets(), "mplus1cthin.ttf"));
        mTitleTv.setTextColor(ContextCompat.getColor(this, R.color.yellow));

        Timer timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    public void run() {
                        startActivity(new Intent().setClass(SplashActivity.this, LoginActivity.class));
                        finish();
                    }
                }, 1000);
    }
}
