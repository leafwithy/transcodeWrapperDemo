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
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

/**
 * Created by weizheng.huang on 2019-10-14.
 */
public class MainActivity extends Activity {

    private TranscodeWrapperDemo2 transcodeWrapperDemo;
    private DemuxAndMux demuxAndMux;
    private EditText startTimeS;
    private EditText startTimeM;
    private EditText endTimeM;
    private EditText endTimeS;
    private EditText fileSize;
    private long startTime;
    private long endTime;
    private double fileRate = 1.0;
    private boolean isStarted = false;
    private AssetFileDescriptor srcFilePath;
    private AssetFileDescriptor srcFilePath2;
    private String filePath;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        Button btn = findViewById(R.id.btn);
        Button pauseBtn = findViewById(R.id.pauseTranscodeBtn);
        Button muxBtn = findViewById(R.id.mux);
        startTimeS = findViewById(R.id.secondsOfStartTime);
        startTimeM = findViewById(R.id.minuteOfStartTime);
        endTimeM = findViewById(R.id.minuteOfEndTime);
        endTimeS = findViewById(R.id.secondsOfEndTime);
        fileSize = findViewById(R.id.FileSize);
        srcFilePath2 = getResources().openRawResourceFd(R.raw.shape_of_my_heart2);
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shape2.mp4";
        srcFilePath = getResources().openRawResourceFd(R.raw.shape_of_my_heart);
        String SM = String.valueOf(startTimeM.getText()) ;
        String SS = String.valueOf(startTimeS.getText());
        String EM = String.valueOf(endTimeM.getText());
        String ES = String.valueOf(endTimeS.getText());
        String FR = String.valueOf(fileSize.getText());
        startTime = Long.valueOf(!SM.equals("") ? SM : "0") * 60 + Long.valueOf(!SS.equals("") ? SS : "0");
        endTime = Long.valueOf(!EM.equals("") ? EM : "0") * 60 + Long.valueOf(!ES.equals("") ? ES : "0");
        fileRate = Double.valueOf(!FR.equals("") ? FR : "100") / 100.0;
        initTranscode();
        initDemuxAndMux();

        muxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyPermission(MainActivity.this)) {
                    demuxAndMux.startDemuxAndMux();
                }
            }
        });
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

    private void initDemuxAndMux(){

        demuxAndMux = new DemuxAndMux(srcFilePath,filePath);
        demuxAndMux.init();
    }
    private void initTranscode(){

        transcodeWrapperDemo = new TranscodeWrapperDemo2(filePath, srcFilePath,srcFilePath2);
        transcodeWrapperDemo.setAssignSize(fileRate);
        transcodeWrapperDemo.init();
        transcodeWrapperDemo.setTailTime(startTime,endTime);

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





}
