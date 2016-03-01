package com.kawakawaplanning.atsumare.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.kawakawaplanning.atsumare.MainApplication;
import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.activity.MapsActivity;
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
import butterknife.OnClick;

/**
 * Created by KP on 15/12/19.
 */
public class WaitGroupFragment extends Fragment {

    @Bind(R.id.waitLv)
    public ListView mWaitLv;

    private Handler mHandler;
    private Timer mTimer;
    private WaitMemberAdapter mWaitMemberAdapter;
    private List<WaitMemberData> mWmdList;

    private Bitmap mSuccessImage;
    private Bitmap mErrorImage;

    private MainApplication application;
    public static final String TAG = "WaitGroupFragment";

    private boolean usedIntent = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group_wait, container, false);
        ButterKnife.bind(this, v);
        application.setmIsClick(false);
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (MainApplication) getActivity().getApplication();


//        mPref = getActivity().getSharedPreferences("loginPref", Activity.MODE_PRIVATE);
        mHandler = new Handler();
        mWmdList = new ArrayList<>();

        mSuccessImage = BitmapFactory.decodeResource(getResources(), R.drawable.success);
        mErrorImage = BitmapFactory.decodeResource(getResources(), R.drawable.error);

        mTimer = null;
        mTimer = new Timer();

        if (mWaitMemberAdapter != null) {
            mWaitMemberAdapter.clear();
            mWaitMemberAdapter.notifyDataSetChanged();
            mWaitLv.setAdapter(mWaitMemberAdapter);
        }

        mHandler.post(() -> firstCheck(application.getGroupId()));
    }



    @Override
    public void onStart() {
        super.onStart();
        new HttpConnector("grouplogin", "{\"user_id\":\"" + application.getMyId() + "\",\"group_id\":\"" + application.getGroupId() + "\"}").post();
        Log.v("kp", "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v("kp", "onStop");
        if(!usedIntent){
            new HttpConnector("grouplogout", "{\"user_id\":\"" + application.getMyId() + "\",\"group_id\":\"" + application.getGroupId() + "\"}").post();
        }
        mTimer.cancel();
    }

    public void firstCheck(String mGroupId) {
        HttpConnector httpConnector = new HttpConnector("loginstate", "{\"group_id\":\"" + mGroupId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {

            if (isDetached() || getActivity() == null) return;

            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                Boolean flag = true;
                for (int i = 0; i != data.length(); i++) {

                    JSONObject object = data.getJSONObject(i);
                    WaitMemberData item = new WaitMemberData();
                    if (object.getString("login_now").equals(mGroupId) || object.getString("user_id").equals(application.getMyId())) {
                        item.setImagaData(mSuccessImage);
                    } else {
                        item.setImagaData(mErrorImage);
                        flag = false;
                    }

                    item.setTextData(object.getString("user_name") + "(" + object.getString("user_id") + ")");
                    mWmdList.add(item);
                    item = null;
                }
                mWaitMemberAdapter = new WaitMemberAdapter(getActivity(), 0, mWmdList);
                mWaitLv.setAdapter(mWaitMemberAdapter);
                if (json.getInt("using") == 0 || flag) {//すでに始めてるorエラーじゃなければ
                    new HttpConnector("setusing", "{\"group_id\":\"" + mGroupId + "\",\"using\":0}").post();

                    usedIntent = true;

                    Intent intent = new Intent();
                    intent.setClass(getActivity(), MapsActivity.class);
                    Log.v(TAG, mGroupId);

                    startActivity(intent);

                    getActivity().getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    FragmentManager fragmentManager = getActivity().getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack(null);

                    Fragment fragment1 = new SelectGroupFragment();
                    fragmentTransaction.replace(android.R.id.content, fragment1);
                    fragmentTransaction.commit();
                } else {
                    TimerTask task = new TimerTask() {
                        public void run() {
                            mHandler.post(() -> loginCheck(mGroupId));
                        }
                    };
                    try {
                        mTimer.schedule(task, 0, 1000);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        });
        httpConnector.post();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.v(TAG, "onDestroyView");
    }

    @OnClick(R.id.startBtn)
    public void start(View v) {
        usedIntent = true;
        HttpConnector http = new HttpConnector("setusing", "{\"group_id\":\"" + application.getGroupId() + "\",\"using\":0}");
        http.post();
        mTimer.cancel();
        Intent intent = new Intent();
        intent.setClassName("com.kawakawaplanning.atsumare", "com.kawakawaplanning.atsumare.activity.MapsActivity");

        startActivity(intent);

        getActivity().getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentManager fragmentManager = getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);

        Fragment fragment1 = new SelectGroupFragment();
        fragmentTransaction.replace(android.R.id.content, fragment1);
        fragmentTransaction.commit();
    }

    public void loginCheck(String mGroupId) {
        HttpConnector httpConnector = new HttpConnector("loginstate", "{\"group_id\":\"" + mGroupId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {

            if (isDetached() || getActivity() == null) return;

            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                Boolean flag = true;
                for (int i = 0; i != data.length(); i++) {

                    JSONObject object = data.getJSONObject(i);
                    WaitMemberData wmd = mWaitMemberAdapter.getItem(i);
                    if (object.getString("login_now").equals(mGroupId)) {
                        wmd.setImagaData(mSuccessImage);

                    } else {
                        wmd.setImagaData(mErrorImage);
                        flag = false;
                    }

                    mWmdList.set(i, wmd);
                }
                if (json.getInt("using") == 0 || flag) {
                    usedIntent = true;
                    HttpConnector http = new HttpConnector("setusing", "{\"group_id\":\"" + application.getGroupId() + "\",\"using\":0}");
                    http.post();
                    mTimer.cancel();
                    Intent intent = new Intent();
                    intent.setClassName("com.kawakawaplanning.atsumare", "com.kawakawaplanning.atsumare.activity.MapsActivity");

                    startActivity(intent);

                    getActivity().getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                    FragmentManager fragmentManager = getActivity().getFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.addToBackStack(null);

                    Fragment fragment1 = new SelectGroupFragment();
                    fragmentTransaction.replace(android.R.id.content, fragment1);
                    fragmentTransaction.commit();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            mWaitMemberAdapter.notifyDataSetChanged();

        });
        httpConnector.post();
    }

}
