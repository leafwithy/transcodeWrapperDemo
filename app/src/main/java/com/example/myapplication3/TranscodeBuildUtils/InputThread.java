package com.example.myapplication3.TranscodeBuildUtils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.util.Log;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

/**
 * Created by weizheng.huang on 2019-10-30.
 */
class InputThread extends  Thread{

    private boolean isNeedTailed = false;
    private boolean pauseInput = false;
    private int formatIndex;
    private double totalMS;
    private long startTime = 0;
    private long endTime = 0;
    private long TIME_US = 50000l;
    private MediaExtractor extractor;
    private MediaCodec decodec;
    private MediaCodec encodec;

    public InputThread(@Nullable MediaExtractor extractor , @Nullable  MediaCodec decodec , @Nullable  MediaCodec encodec , int formatIndex ,double totalMS){
        this.extractor = extractor;
        this.decodec = decodec;
        this.encodec = encodec;
        this.formatIndex = formatIndex;
        this.totalMS = totalMS;
    }

    public void setPauseInput(boolean pauseInput) {
        this.pauseInput = pauseInput;
    }

    public void setTIME_US(long TIME_US) {
        this.TIME_US = TIME_US;
    }


    public void setTailTime(long startTime,long endTime){
        long s = 0;
        long e = (long)totalMS;
        startTime *= 1000000;
        endTime *= 1000000;
        this.startTime = startTime > s ? startTime : s;

        this.endTime = (endTime < e) && (endTime != 0) ? endTime : e;
        this.isNeedTailed = (startTime > 0) && (endTime < e);

    }

    @Override
    public void run() {
        inputLoop();
        extractor.release();
        decodec.stop();
        decodec.release();
        Log.v("tag","released decode");
    }



    private void inputLoop(){
        extractor.selectTrack(formatIndex);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean closeExtractor = false;
        if (isNeedTailed){
            extractor.seekTo(startTime,MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }

        while(!Thread.interrupted()){
            if (!closeExtractor){
                int inputIndex = decodec.dequeueInputBuffer(TIME_US);
                if (inputIndex >= 0){
                    ByteBuffer inputBuffer = decodec.getInputBuffer(inputIndex);
                    int size = extractor.readSampleData(inputBuffer,0);
                    if (size < 0){
                        decodec.queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        closeExtractor = true;
                    }else{
                        if (extractor.getSampleTime() > endTime && isNeedTailed){
                            decodec.queueInputBuffer(inputIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            closeExtractor = true;
                        }else{
                            decodec.queueInputBuffer(inputIndex,0,size,extractor.getSampleTime(),extractor.getSampleFlags());
                            extractor.advance();
                        }
                    }
                }
            }

            int outputIndex = decodec.dequeueOutputBuffer(info,TIME_US);
            if (outputIndex >= 0){
                ByteBuffer outputBuffer = decodec.getOutputBuffer(outputIndex);
                int inputBufferEncodeIndex = encodec.dequeueInputBuffer(TIME_US);
                if (inputBufferEncodeIndex >= 0){
                    if (info.size < 0){
                        encodec.queueInputBuffer(inputBufferEncodeIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }else{
                        ByteBuffer inputEncodeBuffer = encodec.getInputBuffer(inputBufferEncodeIndex);
                        inputEncodeBuffer.put(outputBuffer);
                        encodec.queueInputBuffer(inputBufferEncodeIndex,0,info.size,info.presentationTimeUs,info.flags);
                    }
                }
                decodec.releaseOutputBuffer(outputIndex,false);
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                Log.d("tag","start release Decode");
                break;
            }
            while(pauseInput){
                try {
                    Thread.sleep(10l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
