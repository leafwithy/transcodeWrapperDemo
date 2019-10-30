package com.example.myapplication3.TranscodeBuildUtils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaMuxer;

import androidx.annotation.Nullable;

/**
 * Created by weizheng.huang on 2019-10-30.
 */
public class TranscodeBuilder {
    private Transcode transcode = new Transcode();

    public void buildVideoInputThread(@Nullable MediaExtractor extractor , @Nullable MediaCodec decodec , @Nullable  MediaCodec encodec , int formatIndex , double totalMS){
        transcode.setVideoInputThread(extractor, decodec, encodec, formatIndex, totalMS);
    }
    public void buildAudioInputThread(@Nullable MediaExtractor extractor , @Nullable  MediaCodec decodec , @Nullable  MediaCodec encodec , int formatIndex ,double totalMS){
        transcode.setAudioInputThread(extractor, decodec, encodec, formatIndex, totalMS);
    }
    public void buildVideoOutputThread(@Nullable MediaMuxer muxer , @Nullable MediaCodec encodec , @Nullable String MIME , double totalMS){
        transcode.setVideoOutputThread(muxer, encodec, MIME, totalMS);
    }
    public void buildAudioOutputThread(@Nullable MediaMuxer muxer , @Nullable MediaCodec encodec ,@Nullable String MIME , double totalMS){
        transcode.setAudioOutputThread(muxer, encodec, MIME, totalMS);
    }
    public void setTIMEUS(long TIME_US){
        transcode.setTimeUS(TIME_US);
    }

    public Transcode build(){
        return transcode;
    }
}

