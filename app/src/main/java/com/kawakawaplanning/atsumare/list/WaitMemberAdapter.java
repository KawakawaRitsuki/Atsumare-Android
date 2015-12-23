package com.kawakawaplanning.atsumare.list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kawakawaplanning.atsumare.R;

import java.util.List;

/**
 * Created by KP on 15/12/15.
 */
public class WaitMemberAdapter extends ArrayAdapter<WaitMemberData> {
    private LayoutInflater layoutInflater_;

    public WaitMemberAdapter(Context context, int textViewResourceId, List<WaitMemberData> objects) {
        super(context, textViewResourceId, objects);
        layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    //以下2つをfalseで返すと選択が行えなくなる
    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // 特定の行(position)のデータを得る
        WaitMemberData item = (WaitMemberData) getItem(position);

        // convertViewは使い回しされている可能性があるのでnullの時だけ新しく作る
        if (null == convertView) {
            convertView = layoutInflater_.inflate(R.layout.custom_list_layout, null);
        }

        // WaitMemberDataのデータをViewの各Widgetにセットする
        ImageView imageView;
        imageView = (ImageView) convertView.findViewById(R.id.image);
        imageView.setImageBitmap(item.getImageData());

        TextView textView;
        textView = (TextView) convertView.findViewById(R.id.text);
        textView.setText(item.getTextData());

        return convertView;
    }
}
