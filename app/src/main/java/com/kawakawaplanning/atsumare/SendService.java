package com.kawakawaplanning.atsumare;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.kawakawaplanning.atsumare.activity.MapsActivity;
import com.kawakawaplanning.atsumare.http.HttpConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by KP on 15/12/24.
 */

public class SendService extends Service implements LocationListener {

    private LocationManager locationManager;

    Timer mTimer;

    HashMap<String, Integer> mMap;
    NotificationManager mNm;

       SharedPreferences pref;

    @Override
    public void onCreate() {
        Log.v("kp","test");


        mMap = new HashMap<>();
        pref = getSharedPreferences("loginPref", Activity.MODE_PRIVATE);

        mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this); // 位置情報リスナー

        mTimer = new Timer();
        mTimer.schedule(
                new TimerTask() {
                    public void run() {
                        new Thread(SendService.this::loginCheck).start();
                    }
                }, 1000, 1000);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        locationManager.removeUpdates(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        sendLocate(location.getLatitude(),location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(String provider){}
    @Override
    public void onProviderDisabled(String provider){}


    public void sendLocate(final double lat ,final double lon){

        if (lat != 0.0) {
            HttpConnector httpConnector = new HttpConnector("regist","{\"user_id\":\"" + pref.getString("MyId", "") + "\",\"latitude\":\"" + lat + "\",\"longitude\":\"" + lon + "\"}");
            httpConnector.post();
        }
    }

    public void loginCheck() {
        HttpConnector httpConnector = new HttpConnector("logincheck","{\"group_id\":\"" + pref.getString("GroupId","") + "\"}");
        httpConnector.setOnHttpResponseListener((response) -> {
            try {
                JSONObject json = new JSONObject(response);
                JSONArray data = json.getJSONArray("data");

                for (int i = 0; i != data.length(); i++) {
                    JSONObject object = data.getJSONObject(i);
                    if (mMap.containsKey(object.getString("user_id"))) {
                        if (!object.getString("user_id").equals(pref.getString("MyId", "")) && mMap.get(object.getString("user_id")) != object.getInt("login")) {
                            if (object.getInt("login") == 1)
                                notification(object.getString("user_name"), "ログアウト");
                            else
                                notification(object.getString("user_name"), "ログイン");
                        }
                    }
                    mMap.put(object.getString("user_id"), object.getInt("login"));
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
        });


        httpConnector.postNoHandler();
    }

    public void notification(String who,String what){

        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, new Intent(this, MapsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(this)
                .setTicker(who + "さんが" + what + "しました。")
                .setContentTitle("集まれ！")
                .setContentText(who + "さんが" + what + "しました。")
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setAutoCancel(true)
                .build();

        int nId = R.string.app_name + 1;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(nId, notification);
    }
}