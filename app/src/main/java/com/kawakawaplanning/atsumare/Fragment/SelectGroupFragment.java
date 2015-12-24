package com.kawakawaplanning.atsumare.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.http.HttpConnector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectGroupFragment extends Fragment {

    @Bind(R.id.logoutBtn)
    public Button mLogoutBtn;
    @Bind(R.id.inGroupBtn)
    public Button mInGroupBtn;
    @Bind(R.id.makeGroupBtn)
    public Button mMakeGroupBtn;
    @Bind(R.id.groupLv)
    public ListView mGroupLv;

    private SharedPreferences mPref;
    private String mMyId;
    private Handler mHandler;
    private ProgressDialog mWaitDialog; 


    public SelectGroupFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_group_select, container, false);
        ButterKnife.bind(this,v);
        return v;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPref = getActivity().getSharedPreferences("loginPref", Activity.MODE_PRIVATE);
        mMyId = mPref.getString("loginId", "");
        mHandler = new Handler();

    }

    @Override
    public void onResume() {
        super.onResume();
        listLoad();
    }

    @OnClick(R.id.logoutBtn)
    public void logout(View v){

        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(getActivity());
        adb.setTitle("確認");
        adb.setMessage("ログアウトしますか？");
        adb.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
            SharedPreferences.Editor editor = mPref.edit();
            editor.putString("username", "");
            editor.putString("password", "");
            editor.putBoolean("AutoLogin", false);
            editor.apply();
            getActivity().finish();
        });
        adb.setNegativeButton("Cancel", null);
        adb.show();

    }

    @OnClick(R.id.makeGroupBtn)
    public void makeGroup(View v){
        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(getActivity().LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.dialog_et1, (ViewGroup)getActivity().findViewById(R.id.dialog_layout));

        final EditText et1 = (EditText)view.findViewById(R.id.editText1);
        final TextView tv1 = (TextView)view.findViewById(R.id.dig_tv1);

        tv1.setText("作成するグループ名を入力してください。");

        alertDialogBuilder.setTitle("グループ作成");
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
            final String str = et1.getEditableText().toString();
            if (!str.isEmpty()) {
                HttpConnector httpConnector = new HttpConnector("makegroup", "{\"user_id\":\"" + mMyId + "\",\"group_name\":\"" + str + "\"}");
                httpConnector.setOnHttpResponseListener((String message) -> {
                    showAlert("グループ作成完了", "グループの作成が完了しました。友達を早速誘おう！グループIDは「" + message + "」です。");
                    listLoad();
                });
                httpConnector.setOnHttpErrorListener((int error) -> showAlert("接続エラー","接続エラーが発生しました。インターネットの接続状態を確認して下さい。"));
                httpConnector.post();
            }
        });
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.show();
    }

    private AdapterView.OnItemClickListener onItem = (AdapterView<?> parent, View view, int position, long id) -> {
        Map<String, String> map = (Map<String, String>) parent.getAdapter().getItem(position);
        FragmentManager fragmentManager = getActivity().getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.addToBackStack(null);

        Fragment fragment1 = WaitGroupFragment.newInstance(map.get("Member").substring(7),mMyId);
        fragmentTransaction.replace(android.R.id.content, fragment1);
        fragmentTransaction.commit();
    };

    private AdapterView.OnItemLongClickListener onItemLong = (final AdapterView<?> parent, View view, final int position, long id) -> {
            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(getActivity());
            adb.setCancelable(true);
            adb.setTitle("確認");
            adb.setMessage("このグループから退出しますか？");
            adb.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
                    showWait("処理");
                    Map<String, String> map = (Map<String, String>) parent.getAdapter().getItem(position);
                    HttpConnector httpConnector = new HttpConnector("outgroup", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"" + map.get("Member").substring(7) + "\"}");
                    httpConnector.setOnHttpResponseListener((String message) -> {
                        Log.v("tag", message);
                        mWaitDialog.dismiss();
                        if (Integer.parseInt(message) == 0) {
                            showAlert("グループ退出", "グループ退出完了しました。");
                        } else {
                            showAlert("エラー","エラーが発生しました。時間を開けて試してください。それでもダメな場合はサポートに連絡してください。");
                        }
                        listLoad();
                    });
                    httpConnector.setOnHttpErrorListener((int error) -> {
                        mWaitDialog.dismiss();
                        showAlert("接続エラー","接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                    });
                    httpConnector.post();
            });
            adb.setNegativeButton("Cancel", null);
            adb.show();
            return true;
    };

    @OnClick(R.id.inGroupBtn)
    public void inGroup(View v){
        android.support.v7.app.AlertDialog.Builder alertDialogBuilder = new android.support.v7.app.AlertDialog.Builder(getActivity());
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view =  inflater.inflate(R.layout.dialog_et1,(ViewGroup)getActivity().findViewById(R.id.dialog_layout));

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
                showWait("グループ検索");

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
                                        showAlert("グループ参加", "グループに参加しました。早速グループを選択して遊ぼう！");
                                    } else {
                                        showAlert("エラー", "グループが見つかりませんでした。グループIDを確認してからもう一度お試しください。");
                                    }
                                    listLoad();
                                });
                                httpCon.setOnHttpErrorListener((int error) -> showAlert("接続エラー", "接続エラーが発生しました。インターネットの接続状態を確認して下さい。"));
                                httpCon.post();
                            } else {
                                showAlert("エラー", "すでに参加しています。グループIDを確認してもう一度お試しください。");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        mWaitDialog.dismiss();
                        HttpConnector httpCon = new HttpConnector("ingroup", "{\"user_id\":\"" + mMyId + "\",\"group_id\":\"" + str + "\"}");
                        httpCon.setOnHttpResponseListener((String message) -> {
                            if (Integer.parseInt(message) == 0) {
                                showAlert("グループ参加", "グループに参加しました。さっそくグループを選択して遊ぼう！");
                                listLoad();
                            } else {
                                showAlert("エラー", "グループが見つかりませんでした。グループIDを確認してもう一度お試しください。");
                            }
                        });
                        httpCon.setOnHttpErrorListener((int error) -> showAlert("接続エラー", "接続エラーが発生しました。インターネットの接続状態を確認して下さい。"));
                        httpCon.post();
                    }

                });
                httpConnector.setOnHttpErrorListener((int error) -> showAlert("接続エラー", "接続エラーが発生しました。インターネットの接続状態を確認して下さい。"));
                httpConnector.post();
            }
        });

        alertDialogBuilder.show();
    }


    private List list;
    private void listLoad() {
        showWait("グループ読み込み");
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
                SimpleAdapter adapter = new SimpleAdapter(getActivity(), list, android.R.layout.simple_list_item_2, new String[]{"Name", "Member"}, new int[]{android.R.id.text1, android.R.id.text2});
                mGroupLv.setAdapter(adapter);
                mGroupLv.setOnItemClickListener(onItem);
                mGroupLv.setOnItemLongClickListener(onItemLong);
            });
        });
        httpConnector.setOnHttpErrorListener((int error) -> {
            mWaitDialog.dismiss();
            showAlert("接続エラー","接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
        });
        httpConnector.post();
    }

    public void showAlert(String title,String text){
        android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(getActivity());
        adb.setCancelable(true);
        adb.setTitle(title);
        adb.setMessage(text);
        adb.setPositiveButton("OK", null);
        adb.show();
    }

    public void showWait(String what){
        mWaitDialog = new ProgressDialog(getActivity());
        mWaitDialog.setMessage(what + "中...");
        mWaitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitDialog.setCanceledOnTouchOutside(false);
        mWaitDialog.show();
    }
}
