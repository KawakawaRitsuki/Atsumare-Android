package com.kawakawaplanning.atsumare.Fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.http.HttpConnector;
import com.kawakawaplanning.atsumare.list.WaitMemberAdapter;
import com.kawakawaplanning.atsumare.list.WaitMemberData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by KP on 15/12/19.
 */
public class WaitGroupFragment  extends Fragment {

    @Bind(R.id.waitLv)
    public ListView mWaitLv;

    private String mGroupId;
    private String mMyId;

    private Handler mHandler;
    private SharedPreferences mPref;
    private Timer mTimer;
    private WaitMemberAdapter mWaitMemberAdapter;
    private List<WaitMemberData> mWmdList;

    public static final String TAG = "WaitGroupFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group_wait, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPref = getActivity().getSharedPreferences("loginmPref", Activity.MODE_PRIVATE);
        mHandler = new Handler();
        mWmdList = new ArrayList<>();

        mTimer = null;
        mTimer = new Timer();

        if(mWaitMemberAdapter != null) {
            mWaitMemberAdapter.clear();
            mWaitMemberAdapter.notifyDataSetChanged();
            mWaitLv.setAdapter(mWaitMemberAdapter);
        }

        mHandler.post(() -> firstCheck(mGroupId));

        HttpConnector httpConnector = new HttpConnector("grouplogin","{\"user_id\":\""+mMyId+"\",\"group_id\":\""+mGroupId+"\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            if (Integer.parseInt(message) == 1) {
                Toast.makeText(getActivity(), "サーバーエラーが発生しました。時間を開けてお試しください。", Toast.LENGTH_SHORT).show();
            }
        });
        httpConnector.post();
    }

    @Override
    public void onStop() {
        super.onStop();
        mTimer.cancel();
    }

    public void firstCheck(String mGroupId){
        HttpConnector httpConnector = new HttpConnector("loginstate","{\"group_id\":\"" + mGroupId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {

            if(isDetached() || getActivity() == null)return;

            Bitmap successImage = BitmapFactory.decodeResource(getResources(), R.drawable.success);
            Bitmap errorImage = BitmapFactory.decodeResource(getResources(), R.drawable.error);

            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                Boolean flag = true;
                for (int i = 0; i != data.length(); i++) {

                    JSONObject object = data.getJSONObject(i);
                    WaitMemberData item = new WaitMemberData();
                    if(object.getString("login_now").equals(mGroupId) || object.getString("user_id").equals(mMyId)) {
                        item.setImagaData(successImage);
                    } else {
                        item.setImagaData(errorImage);
                        flag=false;
                    }

                    item.setTextData(object.getString("user_name") + "(" + object.getString("user_id") + ")");
                    mWmdList.add(item);
                }
                mWaitMemberAdapter = new WaitMemberAdapter(getActivity(),0, mWmdList);
                mWaitLv.setAdapter(mWaitMemberAdapter);//変更部分
                if(json.getInt("using") == 0 || flag){
                    HttpConnector http = new HttpConnector("setusing","{\"group_id\":\"" + mGroupId + "\",\"using\":0}");
                    http.post();

                    Intent intent = new Intent();
                    intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
                    mPref.edit().putString("mGroupId",mGroupId).apply();
                    mPref.edit().putString("loginid",mMyId).apply();
                    Log.v("kp", mGroupId);
                    startActivity(intent);
                }else{
                    TimerTask task = new TimerTask() {
                        public void run() {
                            mHandler.post(() -> loginCheck(mGroupId));
                        }
                    };
                    try {
                        mTimer.schedule(task, 0, 1000);
                    }catch (IllegalStateException e){
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        });
        httpConnector.post();
    }

    public static Fragment newInstance(String groupId,String myId) {
        WaitGroupFragment fragment = new WaitGroupFragment();
        fragment.mGroupId = groupId;
        fragment.mMyId = myId;
        return fragment;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.v(TAG,"onDestroyView");
    }

    public void start(View v){
        HttpConnector http = new HttpConnector("setusing","{\"group_id\":\"" + mGroupId + "\",\"using\":0}");
        http.post();
        mTimer.cancel();
        Intent intent = new Intent();
        intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
        mPref.edit().putString("mGroupId",mGroupId).apply();
        mPref.edit().putString("loginid",mMyId).apply();
        startActivity(intent);
    }

    public void loginCheck(String mGroupId){
        HttpConnector httpConnector = new HttpConnector("loginstate","{\"group_id\":\"" + mGroupId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {

            if(isDetached() || getActivity() == null)return;

            Bitmap successImage = BitmapFactory.decodeResource(getResources(), R.drawable.success);
            Bitmap errorImage = BitmapFactory.decodeResource(getResources(), R.drawable.error);

            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                Boolean flag = true;
                for (int i = 0; i != data.length(); i++) {

                    JSONObject object = data.getJSONObject(i);
                    WaitMemberData wmd = mWaitMemberAdapter.getItem(i);
                    if(object.getString("login_now").equals(mGroupId)) {
                        wmd.setImagaData(successImage);

                    } else {
                        wmd.setImagaData(errorImage);
                        flag=false;
                    }

                    mWmdList.set(i, wmd);
                }
                if(json.getInt("using") == 0 || flag){
                    HttpConnector http = new HttpConnector("setusing","{\"group_id\":\"" + mGroupId + "\",\"using\":0}");
                    http.post();

                    mTimer.cancel();
                    Intent intent = new Intent();
                    intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
                    mPref.edit().putString("mGroupId",mGroupId).apply();
                    mPref.edit().putString("loginid",mMyId).apply();
                    startActivity(intent);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            mWaitMemberAdapter.notifyDataSetChanged();

        });
        httpConnector.post();
    }

}
