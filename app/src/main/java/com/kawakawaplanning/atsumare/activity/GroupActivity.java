package com.kawakawaplanning.atsumare.activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.fragment.SelectGroupFragment;

public class GroupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        SelectGroupFragment fragment1 = new SelectGroupFragment();
        fragmentTransaction.replace(R.id.FrameLayout, fragment1);

        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    getFragmentManager().popBackStack();
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }
}
