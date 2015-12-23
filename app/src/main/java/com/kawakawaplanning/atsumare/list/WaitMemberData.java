package com.kawakawaplanning.atsumare.list;

import android.graphics.Bitmap;

/**
 * Created by KP on 15/12/15.
 */
public class WaitMemberData {
    private Bitmap imageData_;
    private String textData_;

    public void setImagaData(Bitmap image) {
        imageData_ = image;
    }

    public Bitmap getImageData() {
        return imageData_;
    }

    public void setTextData(String text) {
        textData_ = text;
    }

    public String getTextData() {
        return textData_;
    }
}