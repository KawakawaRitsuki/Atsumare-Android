package com.kawakawaplanning.atsumare.Fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kawakawaplanning.atsumare.R;

import butterknife.ButterKnife;

/**
 * Created by KP on 15/12/19.
 */
public class WaitGroupFragment  extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_group_wait, container, false);
        ButterKnife.bind(this, v);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

}
