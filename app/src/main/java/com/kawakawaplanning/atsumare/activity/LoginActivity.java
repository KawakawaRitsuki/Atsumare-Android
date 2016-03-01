package com.kawakawaplanning.atsumare.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.kawakawaplanning.atsumare.MainApplication;
import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.http.HttpConnector;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

//    @Bind(R.id.idEt)
//    public EditText mIdEt;
//    @Bind(R.id.pwEt)
//    public EditText mPwEt;
//    @Bind(R.id.autoCb)
//    public CheckBox mAutoCb;
//    @Bind(R.id.signUpBtn)
//    public Button mSignUpBtn;
//    @Bind(R.id.loginBtn)
//    public Button mLoginUpBtn;
    @Bind(R.id.atsumare)
    public TextView mAtsumareTv;

    @Bind(R.id.twitter_login_button)
    public TwitterLoginButton mTwitterLoginBtn;
    @Bind(R.id.facebook_login_button)
    public LoginButton mFacebookLoginBtn;

    private Vibrator mVib;
    private SharedPreferences mPref;
    private ProgressDialog mWaitDialog;
    private CallbackManager mCallbackManager;
    private MainApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        application = (MainApplication) this.getApplication();

        application.init();

        mPref = getSharedPreferences("loginPref", Activity.MODE_PRIVATE );
        mAtsumareTv.setTypeface(Typeface.createFromAsset(getAssets(), "mplus1cthin.ttf"));

        mVib = (Vibrator) getSystemService (VIBRATOR_SERVICE);

        mTwitterLoginBtn.setOnClickListener((View v) -> waitDig("ログイン"));
        mCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginBtn.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                HttpConnector httpConnector = new HttpConnector("twittercheck", "{\"user_id\":\"" + "fb_" + loginResult.getAccessToken().getUserId() + "\"}");
                httpConnector.setOnHttpResponseListener((String response) -> {
                    if (response.equals("0")) {

                        application.setMyId( "fb_" + loginResult.getAccessToken().getUserId());

                        Intent intent = new Intent();
                        intent.setClass(LoginActivity.this, GroupActivity.class);
                        startActivity(intent);
                    } else {

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoginActivity.this);
                        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.dialog_et1, (ViewGroup) findViewById(R.id.dialog_layout));

                        final EditText et1 = (EditText) view.findViewById(R.id.editText1);
                        final TextView tv1 = (TextView) view.findViewById(R.id.dig_tv1);

                        tv1.setText("はじめまして！\nニックネームを教えて下さい！");

                        alertDialogBuilder.setTitle("ニックネーム設定");
                        alertDialogBuilder.setView(view);
                        alertDialogBuilder.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
                            final String str = et1.getEditableText().toString();
                            if (!str.isEmpty()) {
                                waitDig("登録");
                                HttpConnector httpConnector1 = new HttpConnector("signup", "{\"user_id\":\"" + "fb_" + loginResult.getAccessToken().getUserId() + "\",\"password\":\"\",\"user_name\":\"" + et1.getText().toString() + "\"}");
                                httpConnector1.setOnHttpResponseListener((String message) -> {
                                    mWaitDialog.dismiss();

                                    alert("登録完了", "会員登録が完了しました！OKボタンを押してはじめよう！", (DialogInterface d, int w) -> {
                                        application.setMyId("fb_" + loginResult.getAccessToken().getUserId());
                                        Intent intent = new Intent();
                                        intent.setClass(LoginActivity.this, GroupActivity.class);
                                        startActivity(intent);
                                    });
                                });
                                httpConnector1.setOnHttpErrorListener((int error) -> {
                                    mWaitDialog.dismiss();
                                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(getApplicationContext());
                                    adb.setTitle("接続エラー");
                                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                                    adb.setPositiveButton("OK", null);
                                    adb.setCancelable(true);
                                    adb.show();
                                });
                                httpConnector1.post();
                            }
                        });
                        alertDialogBuilder.setCancelable(true);
                        alertDialogBuilder.show();

                    }
                });
                httpConnector.post();
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });



        mTwitterLoginBtn.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                mWaitDialog.cancel();
                HttpConnector httpConnector = new HttpConnector("twittercheck", "{\"user_id\":\"" + "tw_" + result.data.getUserName() + "\"}");
                httpConnector.setOnHttpResponseListener((String response) -> {
                    if (response.equals("0")) {
                        application.setMyId( "tw_" + result.data.getUserName());
                        Intent intent = new Intent();
                        intent.setClass(LoginActivity.this, GroupActivity.class);
                        startActivity(intent);
                    } else {

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoginActivity.this);
                        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.dialog_et1, (ViewGroup) findViewById(R.id.dialog_layout));

                        final EditText et1 = (EditText) view.findViewById(R.id.editText1);
                        final TextView tv1 = (TextView) view.findViewById(R.id.dig_tv1);

                        tv1.setText("はじめまして！\nニックネームを教えて下さい！");

                        alertDialogBuilder.setTitle("ニックネーム設定");
                        alertDialogBuilder.setView(view);
                        alertDialogBuilder.setPositiveButton("OK", (DialogInterface dialog, int which) -> {
                            final String str = et1.getEditableText().toString();
                            if (!str.isEmpty()) {
                                waitDig("登録");
                                HttpConnector httpConnector1 = new HttpConnector("signup", "{\"user_id\":\"" + "tw_" + result.data.getUserName() + "\",\"password\":\"\",\"user_name\":\"" + et1.getText().toString() + "\"}");
                                httpConnector1.setOnHttpResponseListener((String message) -> {
                                    mWaitDialog.dismiss();

                                    alert("登録完了", "会員登録が完了しました！OKボタンを押してはじめよう！", (DialogInterface d, int w) -> {
                                        application.setMyId( "tw_" + result.data.getUserName());
                                        Intent intent = new Intent();
                                        intent.setClass(LoginActivity.this, GroupActivity.class);
                                        startActivity(intent);
                                    });
                                });
                                httpConnector1.setOnHttpErrorListener((int error) -> {
                                    mWaitDialog.dismiss();
                                    android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(getApplicationContext());
                                    adb.setTitle("接続エラー");
                                    adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
                                    adb.setPositiveButton("OK", null);
                                    adb.setCancelable(true);
                                    adb.show();
                                });
                                httpConnector1.post();
                            }
                        });
                        alertDialogBuilder.setCancelable(true);
                        alertDialogBuilder.show();

                    }
                });
                httpConnector.post();
            }

            @Override
            public void failure(TwitterException exception) {
                mWaitDialog.cancel();
                Log.v("kp", "errored!");
                exception.printStackTrace();
                // Do something on failure
            }
        });

//        tutorial();

        if(mPref.getBoolean("loginNow",false)) {
            Intent intent = new Intent();
            intent.setClass(this, MapsActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        mTwitterLoginBtn.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onCreateOptionsMenu(Menu menu){
        MenuItem item = menu.add(0,0,0,"オープンソースライセンス");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(R.drawable.ic_info_outline_black_24dp);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                Intent i = new Intent();
                i.setClass(this,OSLActivity.class);
                startActivity(i);
                return true;
        }
        return false;
    }

    private void waitDig(String what){
        mWaitDialog = new ProgressDialog(this);
        mWaitDialog.setMessage(what + "中...");
        mWaitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitDialog.setCanceledOnTouchOutside(false);
        mWaitDialog.show();
    }
    private void alert(String til,String msg,DialogInterface.OnClickListener onclick){
        AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
        adb.setTitle(til);
        adb.setMessage(msg);
        adb.setPositiveButton("OK", onclick);
        adb.show();
    }
}
