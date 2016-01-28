package com.kawakawaplanning.atsumare.activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;

import com.kawakawaplanning.atsumare.MainApplication;
import com.kawakawaplanning.atsumare.R;
import com.kawakawaplanning.atsumare.fragment.SelectGroupFragment;
import com.kawakawaplanning.atsumare.http.HttpConnector;

public class GroupActivity extends AppCompatActivity {

    MainApplication application;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        application = (MainApplication) this.getApplication();

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

                    if(application.getCurrentFragment() == 1) {
                        new HttpConnector("grouplogout", "{\"user_id\":\"" + application.getMyId() + "\",\"group_id\":\"" + application.getGroupId() + "\"}").post();
                        application.setCurrentFragment(0);
                    }
                    getFragmentManager().popBackStack();
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

}
