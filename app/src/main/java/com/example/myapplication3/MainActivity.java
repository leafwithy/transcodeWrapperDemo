package com.example.myapplication3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.myapplication3.Utils.CheckListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by weizheng.huang on 2019-10-14.
 */
public class MainActivity extends Activity {

    private EditText startTimeS;
    private EditText startTimeM;
    private EditText endTimeM;
    private EditText endTimeS;
    private EditText fileSize;
    private ListView listView;
    private List<CheckBox> datalist = new ArrayList<CheckBox>();
    private Set<String> fileList = new HashSet<String>();
    private VideoView systemVideo;
    private TranscodeWrapperDemo2 transcodeWrapperDemo;
    private DemuxAndMux demuxAndMux;
    private String filePath;
    private String filePath2;
    private long startTime;
    private long endTime;
    private double fileRate = 1.0;
    public static boolean isStarted = false;
    private AssetFileDescriptor srcFilePath;
    private AssetFileDescriptor srcFilePath2;
    private CheckListAdapter checkListAdapter;

    ////////private or protected//////
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        Button btn = findViewById(R.id.btn);
        Button pauseBtn = findViewById(R.id.pauseTranscodeBtn);
        Button muxBtn = findViewById(R.id.mux);
        Button playerBtn = findViewById(R.id.videoplayer);
        systemVideo = findViewById(R.id.systemVideo);
        listView = findViewById(R.id.listView);
        startTimeS = findViewById(R.id.secondsOfStartTime);
        startTimeM = findViewById(R.id.minuteOfStartTime);
        endTimeM = findViewById(R.id.minuteOfEndTime);
        endTimeS = findViewById(R.id.secondsOfEndTime);
        fileSize = findViewById(R.id.FileSize);
        srcFilePath2 = getResources().openRawResourceFd(R.raw.shape_of_my_heart2);
        filePath2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/mux.mp4";
        srcFilePath = getResources().openRawResourceFd(R.raw.shape_of_my_heart);


        initVideo();
        initListView();
        playerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                systemVideo.setVideoURI(Uri.fromFile(new File(filePath2)));
            }
        });
        muxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyPermission(MainActivity.this)) {
                    demuxAndMux = new DemuxAndMux(fileList, filePath2);
                    demuxAndMux.startDemuxAndMux();
                }

            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (verifyPermission(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, ProgressBarDialog.class));
                    if (!isStarted) {
                        initTranscode();
                        refreshList();
                        isStarted = true;
                        transcodeWrapperDemo.startTranscode();
                    }
                    transcodeWrapperDemo.setPauseTranscode(false);
                }
            }
        });
        pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (transcodeWrapperDemo != null) {
                    refreshList();
                    transcodeWrapperDemo.setPauseTranscode(true);
                }
            }
        });


    }

    private void listAdd(List<CheckBox> list) {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dir = new File(filePath);
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].isDirectory()) {
                String fileName = files[i].getName();
                if (fileName.startsWith("shape")) {
                    CheckBox checkBox = new CheckBox(this);
                    int index = fileName.indexOf("shape");
                    String name = fileName.substring(index);
                    checkBox.setText(name);
                    checkBox.setChecked(false);
                    list.add(checkBox);
                }
            }
        }


    }

    private void initListView() {
        listAdd(datalist);
        checkListAdapter = new CheckListAdapter(datalist, this.getBaseContext());
        checkListAdapter.setCheckListObserver(new CheckListAdapter.CheckListObserver() {
            @Override
            public void checkClick(boolean isChecked, int position) {
                if (isChecked) {
                    fileList.add(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + datalist.get(position).getText());
                } else {
                    fileList.remove(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + datalist.get(position).getText());
                }

            }
        });
        listView.setAdapter(checkListAdapter);
    }

    private void initVideo() {
        MediaController mediaController = new MediaController(this);
        mediaController.show();
        systemVideo.setMediaController(mediaController);

    }

    private void initTranscode() {
        long hour = System.currentTimeMillis() / 1000 / 60 / 60 % 24;
        long second = System.currentTimeMillis() / 1000 / 60 % 60;
        long minute = System.currentTimeMillis() / 1000 % 60;
        filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shape" + hour + ":" + second + ":" + minute + ".mp4";
        File file = new File(filePath);
        if (file.exists()) {
            if (0 >= file.length()) {
                file.delete();
            } else {
                int index = filePath.indexOf("shape");
                String subStr = filePath.substring(0, index + 5);
                hour = System.currentTimeMillis() / 1000 / 60 / 60 % 24;
                second = System.currentTimeMillis() / 1000 / 60 % 60;
                minute = System.currentTimeMillis() / 1000 % 60;
                filePath = subStr + hour + ":" + second + ":" + minute + ".mp4";
            }
        }
        transcodeWrapperDemo = new TranscodeWrapperDemo2(filePath, srcFilePath, srcFilePath2);
        transcodeWrapperDemo.setAssignSize(fileRate);
        transcodeWrapperDemo.init();

        String SM = String.valueOf(startTimeM.getText());
        String SS = String.valueOf(startTimeS.getText());
        String EM = String.valueOf(endTimeM.getText());
        String ES = String.valueOf(endTimeS.getText());
        String FR = String.valueOf(fileSize.getText());
        startTime = Long.valueOf(!SM.equals("") ? SM : "0") * 60 + Long.valueOf(!SS.equals("") ? SS : "0");
        endTime = Long.valueOf(!EM.equals("") ? EM : "0") * 60 + Long.valueOf(!ES.equals("") ? ES : "0");
        fileRate = Double.valueOf(!FR.equals("") ? FR : "100") / 100.0;

        transcodeWrapperDemo.setTailTime(startTime, endTime);

    }

    private boolean verifyPermission(Activity activity) {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
        }
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshList() {
        List<CheckBox> datalist2 = new ArrayList<CheckBox>();
        listAdd(datalist2);
        datalist.clear();
        datalist.addAll(datalist2);
        checkListAdapter.notifyDataSetChanged();
    }

}