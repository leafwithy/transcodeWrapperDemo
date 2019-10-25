package com.example.myapplication3;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by weizheng.huang on 2019-10-25.
 */
public class ProgressBarDialog extends Activity {
    private static MyHandler handler = new MyHandler();
    private static TextView text1;
    private double progress;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog);
        text1= findViewById(R.id.progress);


    }

    public static MyHandler getHandler(){
        if (handler != null){
            handler = new MyHandler();
        }
        return handler;
    }
    public static class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle  bundle = msg.getData();
            String progress = bundle.getString("progress");
            text1.setText(progress+"%");
        }
    }
}
