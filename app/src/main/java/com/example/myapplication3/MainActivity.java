package com.example.myapplication3;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

/**
 * Created by weizheng.huang on 2019-10-14.
 */
public class MainActivity extends Activity {
    private MediaPlayer mediaPlayer;
    private VideoDecoder videoDecoder;
    private AudioDecoder audioDecoder;
    private EncoderMuxer encoderMuxer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        Button btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyPermission(MainActivity.this)) {
                    AssetFileDescriptor srcFilePath = getResources().openRawResourceFd(R.raw.shape_of_my_heart);
                    String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shape.mp4";
                    TranscodeWrapperDemo transcodeWrapperDemo = new TranscodeWrapperDemo(filePath, srcFilePath);
                    transcodeWrapperDemo.startTranscode();
                }
            }
        });


    }

    private boolean verifyPermission(Activity activity){
        String [] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,PERMISSIONS_STORAGE,1);
        }
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    public static void sleepRead(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
