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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
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
    private SharedPreferences.Editor mEditor;
    private ProgressDialog mWaitDialog;
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mAtsumareTv.setTypeface(Typeface.createFromAsset(getAssets(), "mplus1cthin.ttf"));

        mVib = (Vibrator) getSystemService (VIBRATOR_SERVICE);
        mPref = getSharedPreferences("loginPref", Activity.MODE_PRIVATE );

        

        mTwitterLoginBtn.setOnClickListener((View v) -> waitDig("ログイン"));
        mCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginBtn.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Log.v("kp", loginResult.getAccessToken().getToken());
                Log.v("kp", loginResult.getAccessToken().getUserId());
                HttpConnector httpConnector = new HttpConnector("twittercheck", "{\"user_id\":\"" + "fb_" + loginResult.getAccessToken().getUserId() + "\"}");
                httpConnector.setOnHttpResponseListener((String response) -> {
                    if (response.equals("0")) {
                        mEditor = mPref.edit();
                        mEditor.putString("loginId", "fb_" + loginResult.getAccessToken().getUserId());
                        mEditor.apply();
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
                                        mEditor = mPref.edit();
                                        mEditor.putString("loginId", "fb_" + loginResult.getAccessToken().getUserId());
                                        mEditor.apply();
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
                Log.v("kp", result.data.getAuthToken().token);
                Log.v("kp", result.data.getAuthToken().secret);
                Log.v("kp", result.data.getUserName());
                HttpConnector httpConnector = new HttpConnector("twittercheck", "{\"user_id\":\"" + "tw_" + result.data.getUserName() + "\"}");
                httpConnector.setOnHttpResponseListener((String response) -> {
                    if (response.equals("0")) {
                        mEditor = mPref.edit();
                        mEditor.putString("loginId", "tw_" + result.data.getUserName());
                        mEditor.apply();
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
                                        mEditor = mPref.edit();
                                        mEditor.putString("loginId", "tw_" + result.data.getUserName());
                                        mEditor.apply();
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
        }else{
            if(mPref.getBoolean("AutoLogin", false)) {
//                login("自動ログイン", mPref.getString("username", ""), mPref.getString("password", ""));
            }
        }

//        mPwEt.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
//            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
//                login("ログイン", mIdEt.getText().toString(), mPwEt.getText().toString());
//                return true;
//            }
//            return false;
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        mTwitterLoginBtn.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

//    private void tutorial(){
//        ShowcaseConfig config = new ShowcaseConfig();
//        config.setDelay(500);
//        config.setMaskColor(ContextCompat.getColor(this, R.color.showcase_back));
//        config.setContentTextColor(ContextCompat.getColor(this, R.color.showcase_text));
//        config.setDismissTextColor(ContextCompat.getColor(this, R.color.showcase_text));
//
//        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, "login");
//        sequence.setConfig(config);
//        sequence.addSequenceItem(findViewById(R.id.signUpBtn), "まずは会員登録をしましょう！", "次へ");
//        sequence.addSequenceItem(findViewById(R.id.loginBtn), "登録ができたら早速ログイン！", "次へ");
//        sequence.start();
//    }

//    @OnClick(R.id.signUpBtn)
//    public void signUpBtn() {
//        mVib.vibrate(50);
//
//    }
//
//    @OnClick(R.id.loginBtn)
//    public void loginBtn() {
//        mVib.vibrate(50);
//        login("ログイン", mIdEt.getText().toString(), mPwEt.getText().toString());
//    }
//
//    private void login(String msg,String id,String pw){
//        waitDig(msg);
//
//        HttpConnector httpConnector = new HttpConnector("login","{\"user_id\":\"" + id + "\",\"password\":\"" + pw + "\"}");
//        httpConnector.setOnHttpResponseListener((String message) -> {
//            waitDialog.dismiss();
//            if(Integer.parseInt(message) == 0){
//                editor = mPref.edit();
//                editor.putString("loginId", mIdEt.getText().toString());
//                editor.apply();
//                Intent intent = new Intent();
//                intent.setClass(LoginActivity.this, GroupActivity.class);
//                startActivity(intent);
//
//                if (mAutoCb.isChecked()) {
//                    editor = mPref.edit();
//                    editor.putString("username", mIdEt.getText().toString());
//                    editor.putString("password", mPwEt.getText().toString());
//                    editor.putBoolean("AutoLogin", true);
//                    editor.apply();
//                }
//            }else{
//                alert("ログインエラー","IDまたはパスワードが違います。もう一度試してください。エラーコード:1",null);
//            }
//
//
//        });
//        httpConnector.setOnHttpErrorListener((int error) -> {
//            waitDialog.dismiss();
//            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(LoginActivity.this);
//            adb.setTitle("接続エラー");
//            adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
//            adb.setPositiveButton("OK", null);
//            adb.setCancelable(true);
//            adb.show();
//        });
//        httpConnector.post();
//    }

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
