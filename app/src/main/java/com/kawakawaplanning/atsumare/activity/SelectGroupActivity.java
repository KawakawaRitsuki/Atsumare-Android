package com.kawakawaplanning.atsumare.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.http.HttpConnector;
import com.kawakawaplanning.atsumare.list.WaitMemberAdapter;
import com.kawakawaplanning.atsumare.list.WaitMemberData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.OnClick;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class SelectGroupActivity extends AppCompatActivity {

    private String mMyId;
    private String mGroupId;
    private ListView mSelectLv;
    private ListView mWaitLv;
    private ProgressDialog mWaitDialog;
    private Handler mHandler;
    SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPref = getSharedPreferences("loginPref", Activity.MODE_PRIVATE);
        mMyId = mPref.getString("loginId", "");
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setScreenContent(R.layout.fragment_group_select);

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // half second between each showcase view
        config.setMaskColor(getResources().getColor(R.color.showcase_back));
        config.setContentTextColor(getResources().getColor(R.color.showcase_text));
        config.setDismissTextColor(getResources().getColor(R.color.showcase_text));

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, "select");
        sequence.setConfig(config);
        sequence.addSequenceItem(findViewById(R.id.makeGroupBtn), "グループの作成にはこのボタンを押してください\nグループとはこのアプリを同時に使うメンバーをまとめるものです\nグループの「作成」は誰か１人が行います\n\n", "次へ");
        sequence.addSequenceItem(findViewById(R.id.inGroupBtn), "グループに入るにはこのボタンを押してください\nグループを作る１人以外のメンバーは「参加」をしてください", "次へ");
        sequence.start();
    }

    public void logout(View v){
        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(this);
        adb.setTitle("確認");
        adb.setMessage("ログアウトしますか？");
        adb.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
            SharedPreferences.Editor editor = mPref.edit();
            editor.putString("username", "");
            editor.putString("password", "");
            editor.putBoolean("AutoLogin", false);
            editor.apply();
            finish();
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();
    }

    List<Map<String, String>> list;

    private void listLoad() {
        Wait("グループ読み込み");
        list = new ArrayList<>();

        HttpConnector httpConnector = new HttpConnector("getgroup", "{\"user_id\":\"" + mMyId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            mWaitDialog.dismiss();
            try {
                if (!message.equals("notfound")) {
                    JSONObject json = new JSONObject(message);
                    JSONArray data = json.getJSONArray("data");

                    for (int i = 0; i != data.length(); i++) {
                        JSONObject object = data.getJSONObject(i);//ノットファウンド
                        Map<String, String> conMap = new HashMap<>();
                        conMap.put("Name", object.getString("group_name"));
                        conMap.put("Member", "グループID:" + object.getString("group_id"));
                        list.add(conMap);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mHandler.post(() -> {
                SimpleAdapter adapter = new SimpleAdapter(SelectGroupActivity.this, list, android.R.layout.simple_list_item_2, new String[]{"Name", "Member"}, new int[]{android.R.id.text1, android.R.id.text2});
                mSelectLv.setAdapter(adapter);
                mSelectLv.setOnItemClickListener(onItem);
                mSelectLv.setOnItemLongClickListener(onItemLong);
            });
        });
        httpConnector.setOnHttpErrorListener((int error) -> {
            mWaitDialog.dismiss();
            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
            adb.setTitle("接続エラー");
            adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
            adb.setPositiveButton("OK", null);
            adb.setCancelable(true);
            adb.show();
        });
        httpConnector.post();
    }

    @Override
    public boolean dispatchKeyEvent( KeyEvent event) {

        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {

            switch(mScreenId){
                case R.layout.fragment_group_select:
                    logout(null);
                    break;
                case R.layout.fragment_group_wait:
                    HttpConnector httpConnector = new HttpConnector("grouplogin", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"\"}");
                    httpConnector.setOnHttpResponseListener((String message) -> {
                        if (Integer.parseInt(message) == 1) {
                            Toast.makeText(SelectGroupActivity.this, "サーバーエラーが発生しました。時間を開けてお試しください。", Toast.LENGTH_SHORT).show();
                        }
                    });
                    httpConnector.post();
                    mTimer.cancel();
                    setScreenContent(R.layout.fragment_group_select);
                    break;
            }
            return super.dispatchKeyEvent(event);
        }
        return super.dispatchKeyEvent(event);
    }



    private AdapterView.OnItemClickListener onItem = (AdapterView<?> parent, View view, int position, long id) -> {
        Map<String, String> map = (Map<String, String>) parent.getAdapter().getItem(position);
        mGroupId = map.get("Member").substring(7);
        setScreenContent(R.layout.fragment_group_wait);
    };

    private AdapterView.OnItemLongClickListener onItemLong = new AdapterView.OnItemLongClickListener(){

        @Override
        public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
            adb.setCancelable(true);
            adb.setTitle("確認");
            adb.setMessage("このグループを削除しますか？");
            adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Wait("処理");
                    Map<String, String> map = (Map<String, String>) parent.getAdapter().getItem(position);
                    HttpConnector httpConnector = new HttpConnector("outgroup", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"" + map.get("Member").substring(7) + "\"}");
                    httpConnector.setOnHttpResponseListener((String message) -> {
                        Log.v("tag", message);
                        mWaitDialog.dismiss();
                        if (Integer.parseInt(message) == 0) {
                            Toast.makeText(getApplicationContext(), "削除しました", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "エラーが発生しました。時間を開けて試してください。それでもダメな場合はサポートに連絡してください。", Toast.LENGTH_SHORT).show();
                        }
                        listLoad();
                    });
                    httpConnector.setOnHttpErrorListener((int error) -> {
                        mWaitDialog.dismiss();
                        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                        adb.setTitle("接続エラー");
                        adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                        adb.setPositiveButton("OK", null);
                        adb.setCancelable(true);
                        adb.show();
                    });
                    httpConnector.post();
                }
            });
            adb.setNegativeButton("Cancel", null);
            adb.show();
            return true;
        }
    };

    @OnClick(R.id.makeGroupBtn)
    public void makeGroup(View v){
        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(
                LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.dialog_et1,
                (ViewGroup)findViewById(R.id.dialog_layout));

        final EditText et1 = (EditText)view.findViewById(R.id.editText1);
        final TextView tv1 = (TextView)view.findViewById(R.id.dig_tv1);

        tv1.setText("作成するグループ名を入力してください。");

        alertDialogBuilder.setTitle("グループ作成");
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK",(DialogInterface dialog, int which) -> {
            final String str = et1.getEditableText().toString();
            if (!str.isEmpty()) {
                HttpConnector httpConnector = new HttpConnector("makegroup","{\"user_id\":\""+ mMyId +"\",\"group_name\":\""+str+"\"}");
                httpConnector.setOnHttpResponseListener((String message) -> {
                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                    adb.setCancelable(true);
                    adb.setTitle("グループ作成完了");
                    adb.setMessage("グループの作成が完了しました。友達を早速誘おう！グループIDは「" + message + "」です。");
                    adb.setPositiveButton("OK", null);
                    adb.show();
                    listLoad();
                });
                httpConnector.setOnHttpErrorListener((int error) -> {
                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                    adb.setTitle("接続エラー");
                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                    adb.setPositiveButton("OK", null);
                    adb.setCancelable(true);
                    adb.show();
                });
                httpConnector.post();
            }
        });
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.show();
    }


    public void inGroup(View v){
        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(
                LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.dialog_et1,
                (ViewGroup)findViewById(R.id.dialog_layout));

        final EditText et1 = (EditText)view.findViewById(R.id.editText1);
        final TextView tv1 = (TextView)view.findViewById(R.id.dig_tv1);

        tv1.setText("参加したいグループIDを入力してください。");

        alertDialogBuilder.setTitle("グループ参加");
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK", null);
        alertDialogBuilder.setCancelable(true);

        alertDialogBuilder.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
            final String str = et1.getEditableText().toString();
            if (!str.isEmpty()) {
                Wait("グループ検索");

                HttpConnector httpConnector = new HttpConnector("getgroup", "{\"user_id\":\"" + mMyId + "\"}");
                httpConnector.setOnHttpResponseListener((String jsonData) -> {

                    if (!jsonData.equals("notfound")) {
                        mWaitDialog.dismiss();
                        try {
                            JSONObject json = new JSONObject(jsonData);
                            JSONArray data = json.getJSONArray("data");
                            boolean flag = true;
                            for (int i = 0; i != data.length(); i++) {

                                JSONObject object = data.getJSONObject(i);
                                if (object.getString("group_id").equals(str)) {
                                    flag = false;
                                }
                            }
                            if (flag) {
                                HttpConnector httpCon = new HttpConnector("ingroup", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"" + str + "\"}");
                                httpCon.setOnHttpResponseListener((String message) -> {
                                    if (Integer.parseInt(message) == 0) {
                                        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                        adb.setCancelable(true);
                                        adb.setTitle("グループ参加");
                                        adb.setMessage("グループに参加しました。さっそくグループを選択して遊ぼう！");
                                        adb.setPositiveButton("OK", null);
                                        adb.show();
                                        listLoad();
                                    } else {
                                        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                        adb.setCancelable(true);
                                        adb.setTitle("エラー");
                                        adb.setMessage("グループが見つかりませんでした。グループIDを確認してもう一度お試しください。");
                                        adb.setPositiveButton("OK", null);
                                        adb.show();
                                    }
                                });
                                httpCon.setOnHttpErrorListener((int error) -> {
                                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                    adb.setTitle("接続エラー");
                                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                                    adb.setPositiveButton("OK", null);
                                    adb.setCancelable(true);
                                    adb.show();
                                });
                                httpCon.post();
                            } else {
                                android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                adb.setCancelable(true);
                                adb.setTitle("エラー");
                                adb.setMessage("すでに参加しています。グループIDを確認してもう一度お試しください。");
                                adb.setPositiveButton("OK", null);
                                adb.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mWaitDialog.dismiss();
                        HttpConnector httpCon = new HttpConnector("ingroup", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"" + str + "\"}");
                        httpCon.setOnHttpResponseListener((String message) -> {
                            if (Integer.parseInt(message) == 0) {
                                android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                adb.setCancelable(true);
                                adb.setTitle("グループ参加");
                                adb.setMessage("グループに参加しました。さっそくグループを選択して遊ぼう！");
                                adb.setPositiveButton("OK", null);
                                adb.show();
                                listLoad();
                            } else {
                                android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                                adb.setCancelable(true);
                                adb.setTitle("エラー");
                                adb.setMessage("グループが見つかりませんでした。グループIDを確認してもう一度お試しください。");
                                adb.setPositiveButton("OK", null);
                                adb.show();
                            }
                        });
                        httpCon.setOnHttpErrorListener((int error) -> {
                            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                            adb.setTitle("接続エラー");
                            adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                            adb.setPositiveButton("OK", null);
                            adb.setCancelable(true);
                            adb.show();
                        });
                        httpCon.post();
                    }


                });
                httpConnector.setOnHttpErrorListener((int error) -> {
                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(SelectGroupActivity.this);
                    adb.setTitle("接続エラー");
                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                    adb.setPositiveButton("OK", null);
                    adb.setCancelable(true);
                    adb.show();
                });
                httpConnector.post();
            }
        });

        alertDialogBuilder.show();
    }
    private void Wait(String what){
        mWaitDialog = new ProgressDialog(this);
        mWaitDialog.setMessage(what + "中...");
        mWaitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitDialog.setCanceledOnTouchOutside(false);
        mWaitDialog.show();
    }

    int mScreenId = 0;
    private void setScreenContent(int screenId) {
        mScreenId = screenId;
        setContentView(screenId);

        switch (screenId) {
            case R.layout.fragment_group_select: {
                setSelectScreenContent();
                break;
            }
            case R.layout.fragment_group_wait: {
                setWaitScreenContent();
                break;
            }
        }
    }

    private void setSelectScreenContent() {
        mSelectLv = (ListView)findViewById(R.id.groupLv);
        listLoad();
    }

    private Timer mTimer;
    WaitMemberAdapter customAdapter;
    private void setWaitScreenContent() {
        mWaitLv = (ListView)findViewById(R.id.waitLv);

        objects = new ArrayList<>();

        mTimer=null;
        mTimer = new Timer();

        if(customAdapter != null) {
            customAdapter.clear();
            customAdapter.notifyDataSetChanged();
        }

        mWaitLv.setAdapter(customAdapter);

        mHandler.post(() -> firstCheck(mGroupId));



        HttpConnector httpConnector = new HttpConnector("grouplogin","{\"user_id\":\""+ mMyId +"\",\"group_id\":\""+ mGroupId +"\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            if (Integer.parseInt(message) == 1) {
                Toast.makeText(SelectGroupActivity.this, "サーバーエラーが発生しました。時間を開けてお試しください。", Toast.LENGTH_SHORT).show();
            }
        });
        httpConnector.post();

    }
    List<WaitMemberData> objects;
    public void firstCheck(String groupId){
        HttpConnector httpConnector = new HttpConnector("loginstate","{\"group_id\":\"" + groupId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            Bitmap successImage = BitmapFactory.decodeResource(getResources(), R.drawable.success);
            Bitmap errorImage = BitmapFactory.decodeResource(getResources(), R.drawable.error);

            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                Boolean flag = true;
                for (int i = 0; i != data.length(); i++) {

                    JSONObject object = data.getJSONObject(i);
                    WaitMemberData item = new WaitMemberData();
                    if(object.getString("login_now").equals(groupId) || object.getString("user_id").equals(mMyId)) {
                        item.setImagaData(successImage);
                    } else {
                        item.setImagaData(errorImage);
                        flag=false;
                    }

                    item.setTextData(object.getString("user_name") + "(" + object.getString("user_id") + ")");
                    objects.add(item);
                }
                customAdapter = new WaitMemberAdapter(this,0,objects);
                mWaitLv.setAdapter(customAdapter);//変更部分
                if(json.getInt("using") == 0 || flag){
                    HttpConnector http = new HttpConnector("setusing","{\"group_id\":\"" + groupId + "\",\"using\":0}");
                    http.post();

                    Intent intent = new Intent();
                    intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
                    mPref.edit().putString("groupId",groupId).apply();
                    mPref.edit().putString("loginid", mMyId).apply();
                    Log.v("kp",groupId);
                    startActivity(intent);
                }else{
                    TimerTask task = new TimerTask() {
                        public void run() {
                            mHandler.post(() -> loginCheck(groupId));
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

    public void start(View v){
        HttpConnector http = new HttpConnector("setusing","{\"group_id\":\"" + mGroupId + "\",\"using\":0}");
        http.post();
        mTimer.cancel();
        Intent intent = new Intent();
        intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
        mPref.edit().putString("groupId", mGroupId).apply();
        mPref.edit().putString("loginid", mMyId).apply();
        startActivity(intent);
    }

    public void loginCheck(String groupId){
        HttpConnector httpConnector = new HttpConnector("loginstate","{\"group_id\":\"" + groupId + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            Bitmap successImage = BitmapFactory.decodeResource(getResources(), R.drawable.success);
            Bitmap errorImage = BitmapFactory.decodeResource(getResources(), R.drawable.error);

            try {
                JSONObject json = new JSONObject(message);
                JSONArray data = json.getJSONArray("data");

                Boolean flag = true;
                for (int i = 0; i != data.length(); i++) {

                    JSONObject object = data.getJSONObject(i);
                    WaitMemberData cd = customAdapter.getItem(i);
                    if(object.getString("login_now").equals(groupId)) {
                        cd.setImagaData(successImage);

                    } else {
                        cd.setImagaData(errorImage);
                        flag=false;
                    }

                    objects.set(i, cd);
                }
                if(json.getInt("using") == 0 || flag){
                    HttpConnector http = new HttpConnector("setusing","{\"group_id\":\"" + groupId + "\",\"using\":0}");
                    http.post();

                    mTimer.cancel();
                    Intent intent = new Intent();
                    intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
                    mPref.edit().putString("groupId",groupId).apply();
                    mPref.edit().putString("loginid", mMyId).apply();
                    startActivity(intent);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            customAdapter.notifyDataSetChanged();

        });
        httpConnector.post();
    }
}
