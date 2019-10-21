package com.example.myapplication3;

import android.os.Environment;

/**
 * Created by weizheng.huang on 2019-10-15.
 */
public class FileConfig {
    public static String videoFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/1.yuv";
    public static String audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/3.pcm";
    public static int length = -1;

    public static class VideoConfig{
       // public static int infoSize = -1;
        public static String mimeType = null;
        public static int width = -1;
        public static int height = -1;
        public static int frameRate = -1;
        public static int iFrame_inteval = -2;
        public static int bitRate = -1;
    }
    public static class AudioConfig{
     //   public static int infoSize = -1;
        public static int chanelCount = -1;
        public static String mimeType = null;
        public static int bitRate = -1;
        public static int sampleRate = -1;
    }
}
