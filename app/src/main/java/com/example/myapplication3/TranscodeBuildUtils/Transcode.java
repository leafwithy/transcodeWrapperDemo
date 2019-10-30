package com.example.myapplication3.TranscodeBuildUtils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaMuxer;

/**
 * Created by weizheng.huang on 2019-10-30.
 */
public class Transcode {
    private InputThread audioInputThread;
    private OutputThread audioOutputThread;
    private InputThread videoInputThread;
    private OutputThread videoOutputThread;

    void setAudioInputThread(MediaExtractor extractor,MediaCodec decodec,MediaCodec encodec,int formatIndex , double totalMS) {
        this.audioInputThread = new InputThread(extractor,decodec,encodec,formatIndex,totalMS);
    }

    void setAudioOutputThread(MediaMuxer muxer,MediaCodec encodec , String MIME ,double totalMS) {
        this.audioOutputThread = new OutputThread(muxer,encodec,MIME,totalMS);
    }

    void setVideoInputThread(MediaExtractor extractor,MediaCodec decodec , MediaCodec encodec,int formatIndex,double totalMS) {
        this.videoInputThread = new InputThread(extractor,decodec,encodec,formatIndex,totalMS);
    }

    void setVideoOutputThread(MediaMuxer muxer, MediaCodec encodec , String MIME , double totalMS) {
        this.videoOutputThread = new OutputThread(muxer,encodec,MIME,totalMS);
    }

    public void setTimeUS(long TIME_US){
        audioInputThread.setTIME_US(TIME_US);
        audioOutputThread.setTIME_US(TIME_US);
        videoInputThread.setTIME_US(TIME_US);
        videoOutputThread.setTIME_US(TIME_US);
    }

    public void setPauseTranscode(boolean TRUEORFALSE){
        audioOutputThread.setPauseOutput(TRUEORFALSE);
        audioInputThread.setPauseInput(TRUEORFALSE);
        videoInputThread.setPauseInput(TRUEORFALSE);
        videoOutputThread.setPauseOutput(TRUEORFALSE);
    }

    public void setTailTime(long startTime ,long endTime){
        videoInputThread.setTailTime(startTime,endTime);
        audioInputThread.setTailTime(startTime,endTime);
    }

    public void start(){
        videoOutputThread.start();
        audioOutputThread.start();
        videoInputThread.start();
        audioInputThread.start();
    }

}
