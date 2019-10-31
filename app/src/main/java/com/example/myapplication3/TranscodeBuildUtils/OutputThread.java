package com.example.myapplication3.TranscodeBuildUtils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.myapplication3.ProgressBarDialog;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;

/**
 * Created by weizheng.huang on 2019-10-30.
 */
class OutputThread extends Thread{
    private boolean pauseOutput = false;
    private static boolean isMuxerStarted = false;
    private static int videoTrackIndex = -1;
    private static int audioTrackIndex = -1;
    private static int isMuxed = 0;
    private long TIME_US;
    private double totalMS = 0;
    private String MIME;
    private MediaCodec encodec;
    private static MediaMuxer muxer;
    private ProgressBarDialog.MyHandler handler = ProgressBarDialog.getHandler();

    public OutputThread(@Nullable MediaMuxer muxer , @Nullable MediaCodec encodec ,@Nullable String MIME , double totalMS){
        this.muxer = muxer;
        this.encodec = encodec;
        this.MIME = MIME;
        this.totalMS = totalMS;
    }

    public void setPauseOutput(boolean pauseOutput) {
        this.pauseOutput = pauseOutput;
    }

    public void setTIME_US(long TIME_US) {
        this.TIME_US = TIME_US;
    }


    @Override
    public void run() {
        outputLoop();
        encodec.stop();
        encodec.release();
        Log.v("tag","released encodec" + MIME);
        isMuxed++;
        releaseMuxer();
    }


    private void outputLoop(){
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while(!Thread.interrupted()) {
            int outputIndex = encodec.dequeueOutputBuffer(info, TIME_US);
            switch (outputIndex){
                case MediaCodec.INFO_TRY_AGAIN_LATER:{
                  //  Log.d("tag","try again Later");
                    break;
                }
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                {
                    Log.d("tag","format changed " +MIME);
                    MediaFormat format = encodec.getOutputFormat();
                    MIME = format.getString(MediaFormat.KEY_MIME);
                    if (videoTrackIndex < 0 && MIME.startsWith("video"))
                        videoTrackIndex = muxer.addTrack(format);
                    if (audioTrackIndex < 0 && MIME.startsWith("audio"))
                        audioTrackIndex = muxer.addTrack(format);

                    break;
                }
                default:{
                    ByteBuffer outputBuffer = encodec.getOutputBuffer(outputIndex);
                    if (!isMuxerStarted){
                        startMuxer();
                    }
                    if (info.size >= 0 && isMuxerStarted){
                        if (0 < info.presentationTimeUs){
                            double currentMS = info.presentationTimeUs;
                            Bundle bundle = new Bundle();
                            if (MIME.startsWith("video")){
                                bundle.putString("videoProgress",new DecimalFormat(".0").format((currentMS / totalMS) * 100));
                            }
                            if (MIME.startsWith("audio")){
                                bundle.putString("audioProgress",new DecimalFormat(".0").format((currentMS / totalMS) * 100));
                            }
                            Message message = new Message();
                            message.setData(bundle);
                            message.setTarget(handler);
                            handler.sendMessage(message);
                        }
                        int trackIndex = MIME.startsWith("video") ? videoTrackIndex : audioTrackIndex;
                        muxer.writeSampleData(trackIndex , outputBuffer , info);
                        Log.d("tag",MIME + " muxing");
                    }
                    encodec.releaseOutputBuffer(outputIndex,false);
                }
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                Log.d("tag","start release " + MIME + " Encode");
                break;
            }
            while(pauseOutput){
                try {
                    Thread.sleep(10l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static synchronized void startMuxer(){
        if ((0 <= audioTrackIndex ) && (0 <= videoTrackIndex ) && (!isMuxerStarted)){
            muxer.start();
            isMuxerStarted = true;
        }
    }
    private static synchronized void releaseMuxer(){
        if (isMuxed == 2){
            isMuxed++;
            muxer.stop();
            muxer.release();
            Log.v("tag","released muxer");
        }
    }
}
