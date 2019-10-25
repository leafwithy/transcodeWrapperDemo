package com.example.myapplication3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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

    private TranscodeWrapperDemo transcodeWrapperDemo;

    private boolean isStarted = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        Button btn = findViewById(R.id.btn);
        Button pauseBtn = findViewById(R.id.pauseTranscodeBtn);

        initTranscode();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyPermission(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this,ProgressBarDialog.class));
                    if (!isStarted) {
                        isStarted = transcodeWrapperDemo.startTranscode();
                    }
                    transcodeWrapperDemo.setPauseTranscode(false);
                }
            }
        });
        pauseBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                transcodeWrapperDemo.setPauseTranscode(true);
            }
        });




    }


    private void initTranscode(){

        AssetFileDescriptor srcFilePath = getResources().openRawResourceFd(R.raw.shape_of_my_heart);

        AssetFileDescriptor srcFilePath2 = getResources().openRawResourceFd(R.raw.shape_of_my_heart2);
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shape.mp4";
        transcodeWrapperDemo = new TranscodeWrapperDemo(filePath, srcFilePath,srcFilePath2);
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
