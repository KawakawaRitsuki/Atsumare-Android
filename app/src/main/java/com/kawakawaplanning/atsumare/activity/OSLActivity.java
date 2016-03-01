package com.kawakawaplanning.atsumare.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kawakawaplanning.atsumare.R;

/**
 * Created by KP on 16/03/01.
 */
public class OSLActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.opensourcelicense);
        this.setTitle("オープンソースライセンス");
    }
}
