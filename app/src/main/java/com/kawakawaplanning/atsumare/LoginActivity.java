package com.kawakawaplanning.atsumare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.kawakawaplanning.atsumare.http.HttpConnector;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class LoginActivity extends AppCompatActivity {

    @Bind(R.id.idEt)
    public EditText mIdEt;
    @Bind(R.id.pwEt)
    public EditText mPwEt;
    @Bind(R.id.autoCb)
    public CheckBox mAutoCb;
    @Bind(R.id.signUpBtn)
    public Button mSignUpBtn;
    @Bind(R.id.loginBtn)
    public Button mLoginUpBtn;

    private Vibrator mVib;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private ProgressDialog waitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mVib = (Vibrator) getSystemService (VIBRATOR_SERVICE);
        pref = getSharedPreferences("loginpref", Activity.MODE_PRIVATE );

        tutorial();

        if(pref.getBoolean("loginnow",false)) {
            Intent intent = new Intent();
            intent.setClassName("com.kawakawaplanning.gpsdetag", "com.kawakawaplanning.gpsdetag.MapsActivity");
            startActivity(intent);
        }else{
            if(pref.getBoolean("AutoLogin", false)) {
                login("自動ログイン", pref.getString("username", ""), pref.getString("password", ""));
            }
        }

        mPwEt.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                login("ログイン",mIdEt.getText().toString(),mPwEt.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void tutorial(){
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500);
        config.setMaskColor(ContextCompat.getColor(this, R.color.showcase_back));
        config.setContentTextColor(ContextCompat.getColor(this, R.color.showcase_text));
        config.setDismissTextColor(ContextCompat.getColor(this, R.color.showcase_text));

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, "login");
        sequence.setConfig(config);
        sequence.addSequenceItem(findViewById(R.id.signUpBtn), "まずは会員登録をしましょう！", "次へ");
        sequence.addSequenceItem(findViewById(R.id.loginBtn), "登録ができたら早速ログイン！", "次へ");
        sequence.start();
    }

    @OnClick(R.id.signUpBtn)
    public void signUpBtn() {
        mVib.vibrate(50);

    }

    @OnClick(R.id.loginBtn)
    public void loginBtn() {
        mVib.vibrate(50);
        login("ログイン", mIdEt.getText().toString(), mPwEt.getText().toString());
    }

    private void login(String msg,String id,String pw){
        wait(msg);

        HttpConnector httpConnector = new HttpConnector("login","{\"user_id\":\"" + id + "\",\"password\":\"" + pw + "\"}");
        httpConnector.setOnHttpResponseListener((String message) -> {
            waitDialog.dismiss();
            if(Integer.parseInt(message) == 0){
                editor = pref.edit();
                editor.putString("loginid", mIdEt.getText().toString());
                editor.apply();
                Intent intent = new Intent();
                intent.setClass(LoginActivity.this, GroupActivity.class);
                startActivity(intent);

                if (mAutoCb.isChecked()) {
                    editor = pref.edit();
                    editor.putString("username", mIdEt.getText().toString());
                    editor.putString("password", mPwEt.getText().toString());
                    editor.putBoolean("AutoLogin", true);
                    editor.apply();
                }
            }else{
                alert("ログインエラー","IDまたはパスワードが違います。もう一度試してください。エラーコード:1");
            }


        });
        httpConnector.setOnHttpErrorListener((int error) -> {
            waitDialog.dismiss();
            android.support.v7.app.AlertDialog.Builder adb = new android.support.v7.app.AlertDialog.Builder(LoginActivity.this);
            adb.setTitle("接続エラー");
            adb.setMessage("接続エラーが発生しました。インターネットの接続状態を確認して下さい。");
            adb.setPositiveButton("OK", null);
            adb.setCancelable(true);
            adb.show();
        });
        httpConnector.post();
    }
    private void wait(String what){
        waitDialog = new ProgressDialog(this);
        waitDialog.setMessage(what + "中...");
        waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waitDialog.setCanceledOnTouchOutside(false);
        waitDialog.show();
    }
    private void alert(String til,String msg){
        AlertDialog.Builder adb = new AlertDialog.Builder(LoginActivity.this);
        adb.setTitle(til);
        adb.setMessage(msg);
        adb.setPositiveButton("OK", null);
        adb.show();
    }
}
