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
    private static double progress;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_dialog);
        text1= findViewById(R.id.progress);


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (progress == 100){
            finish();
        }
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
            String videoprogress =  bundle.getString("videoProgress");
            String audioprogress = bundle.getString("audioProgress");
            double videoPro = videoprogress == null ? progress : Double.valueOf(videoprogress);
            double audioPro = audioprogress == null ? progress : Double.valueOf(audioprogress);
            progress = videoPro > audioPro ? videoPro : audioPro;
//            String videoPro = bundle.getString("progress");
//            progress = Double.valueOf(videoPro);
            text1.setText(""+progress+"%");
        }
    }
}
