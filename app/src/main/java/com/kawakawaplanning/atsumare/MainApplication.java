package com.kawakawaplanning.atsumare;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.facebook.FacebookSdk;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import io.fabric.sdk.android.Fabric;

/**
 * Created by KP on 15/12/25.
 */
public class MainApplication extends Application {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "358zlvIfDNu4RnrXNJMC47mZr";
    private static final String TWITTER_SECRET = "VL9FQkVpttzcIPxifChjabwnqTXIcvtYKI9hul3hNQNfDkS69O";

    @Override
    public void onCreate() {
        super.onCreate();
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));
        FacebookSdk.sdkInitialize(this);
        FacebookSdk.setApplicationId("711390698997001");
        FacebookSdk.setApplicationName("集まれ！");
    }
    @Override
    public void onTerminate() {
        //終了時
        super.onTerminate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

}
