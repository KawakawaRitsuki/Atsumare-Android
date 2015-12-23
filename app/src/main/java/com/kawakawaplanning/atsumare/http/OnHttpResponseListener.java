package com.kawakawaplanning.atsumare.http;

import java.util.EventListener;

/**
 * Created by KP on 15/08/14.
 */
public interface OnHttpResponseListener extends EventListener {

    void onResponse(String response);

}