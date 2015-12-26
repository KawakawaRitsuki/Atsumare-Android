package com.kawakawaplanning.atsumare.activity;


import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.SendService;
import com.kawakawaplanning.atsumare.http.HttpConnector;
import com.kawakawaplanning.atsumare.list.MapsMemberAdapter;
import com.kawakawaplanning.atsumare.list.MapsMemberData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity {

    static public GoogleMap mGoogleMap;
    static public String mMyId;
    private String mGroupId;
    private Handler mHandler;
    private Map<Integer, Marker> mMarker= new HashMap<>();
    Timer mTimer;
    private NotificationManager mNm;
    SharedPreferences mPref;


    boolean mFinish = false;

    ListView mListLv;
    ImageView mListIv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mListLv = (ListView)findViewById(R.id.listView3);
        mListIv = (ImageView)findViewById(R.id.chatCloseBtn);

        mListIv.setOnClickListener((v) -> {
            mListIv.setVisibility(View.INVISIBLE);
            mListLv.setVisibility(View.INVISIBLE);
        });

        mPref = getSharedPreferences("loginPref", Activity.MODE_PRIVATE);
        mMyId = mPref.getString("loginId", "");
        mGroupId = mPref.getString("groupId", "");

        SharedPreferences.Editor editor = mPref.edit();
        editor.putBoolean("loginNow", true);
        editor.apply();

        mHandler = new Handler();
        mGoogleMap =  ( (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map) ).getMap();

        mapInit();

        if(!isServiceRunning(this,SendService.class))
            startService(new Intent(this, SendService.class));

    }

    public void chatBtn(View v){
        mListIv.setVisibility(View.VISIBLE);
        mListLv.setVisibility(View.VISIBLE);
        HttpConnector httpConnector = new HttpConnector("logincheck", "{\"group_id\":\"" + mGroupId + "\"}");
        httpConnector.setOnHttpResponseListener((message) -> {
            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                List<MapsMemberData> objects = new ArrayList<>();

                for (int i = 0; i != data.length(); i++) {
                    JSONObject object = data.getJSONObject(i);
                    MapsMemberData item = new MapsMemberData();
                    if (object.getInt("login") == 0){
                        item.setColorData(Color.WHITE);
                    }else{
                        item.setColorData(Color.GRAY);
                    }
                    item.setTextData(" " + object.getString("user_name"));
                    objects.add(item);
                }
                MapsMemberAdapter memberAdapter = new MapsMemberAdapter(this,0,objects);
                mListLv.setAdapter(memberAdapter);//変更部分

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
        httpConnector.post();

    }

    public void onClick(View v) {
        AlertDialog.Builder adb = new AlertDialog.Builder(MapsActivity.this);
        adb.setTitle("確認");
        adb.setMessage("終了しますか？");
        adb.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
            mFinish = true;
            HttpConnector httpConnector = new HttpConnector("grouplogout", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"" + mGroupId + "\"}");
            httpConnector.setOnHttpResponseListener((String message) -> {
                Log.v("kp", "Message:" + message);
                if (message.equals("1")) {
                    HttpConnector http = new HttpConnector("setusing", "{\"group_id\":\"" + mGroupId + "\",\"using\":1}");
                    http.post();
                }
                SharedPreferences.Editor editor = mPref.edit();
                editor.putBoolean("loginNow", false);
                editor.apply();
                stopService(new Intent(this, SendService.class));
                finish();
            });
            httpConnector.setOnHttpErrorListener((int error) -> {
                android.support.v7.app.AlertDialog.Builder adbs = new android.support.v7.app.AlertDialog.Builder(MapsActivity.this);
                adbs.setTitle("接続エラー");
                adbs.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                adbs.setPositiveButton("OK", null);
                adbs.setCancelable(true);
                adbs.show();
            });
            httpConnector.post();
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            onClick(null);
            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mTimer = new Timer();
        mTimer.schedule(
                new TimerTask() {
                    public void run() {
                        new Thread(MapsActivity.this::getLocate).start();
                    }
                }, 1000, 1000);

        mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int nId = R.string.app_name;
        mNm.cancel(nId);
        mNm.cancel(nId + 1);
        mMyId = mPref.getString("loginId", "");
    }

    public boolean isServiceRunning(Context c, Class<?> cls) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningService = am.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo i : runningService) {
            if (cls.getName().equals(i.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void mapInit() {
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.setMyLocationEnabled(true);
        CameraPosition cameraPos = new CameraPosition.Builder().target(new LatLng(38.2586, 137.6850)).zoom(4.5f).build();
        mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPos));
    }

    public void getLocate(){
        mHandler.post(() -> {
            HttpConnector httpConnector = new HttpConnector("getlocate", "{\"group_id\":\"" + mGroupId + "\"}");
            httpConnector.setOnHttpResponseListener((String message) -> {
                Log.v("kp", message);
                try {
                    JSONObject json = new JSONObject(message);
                    JSONArray data = json.getJSONArray("data");

                    for (int i = 0; i != data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);
                        setMarker(i, object.getString("user_name"), object.getDouble("latitude"), object.getDouble("longitude"), object.getInt("login"));

                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });
            httpConnector.post();
        });

    }

    public void setMarker(final int id,final String name,final double lat,final double lon,final int login){
        mHandler.post(() -> {
                    if (mMarker.get(id) == null) {
                        if(login==0){
                            LatLng latLng = new LatLng(lat, lon);
                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(latLng);
                            markerOptions.title(name);
                            mMarker.put(id, mGoogleMap.addMarker(markerOptions));
                        }
                    } else {
                        if(login==0) {
                            mMarker.get(id).setVisible(true);
                            LatLng latLng = new LatLng(lat, lon);
                            mMarker.get(id).setPosition(latLng);
                        }else{
                            mMarker.get(id).setVisible(false);
                        }
                    }
                }
        );
    }


    @Override
    protected void onStop() {
        super.onStop();
        mTimer.cancel();
        notification();
        mMyId = null;
    }

    public void notification(){
        if(!mFinish) {

            PendingIntent contentIntent = PendingIntent.getActivity(this, 1, new Intent(this, MapsActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(this)
                    .setTicker("集まれ！は実行中です")
                    .setContentTitle("集まれ！")
                    .setContentText("集まれ！は実行中です。")
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setOngoing(true)
                    .build();

            int nId = R.string.app_name;
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(nId, notification);
        }
    }
}