package com.example.myapplication3;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by weizheng.huang on 2019-10-31.
 */
public class DemuxAndMux {
    private MediaExtractor  extractor;
    private MediaMuxer muxer ;
    private AssetFileDescriptor srcFilePath;
    private String filePath;
    private int videoIndex  = -1;
    private int audioIndex = -1;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private boolean isMuxerStarted = false;
    private int videoInputSize;
    private int audioInputSize;


    public DemuxAndMux(AssetFileDescriptor afd , String filePath){
        this.srcFilePath = afd;
        this.filePath = filePath;
    }

    public void init(){
        initMediaExtractor();
        initMediaMuxer();
    }
    private void initMediaExtractor(){
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(srcFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < extractor.getTrackCount(); i++){
            MediaFormat format = extractor.getTrackFormat(i);
            String MIME = format.getString(MediaFormat.KEY_MIME);
            if (MIME.startsWith("video")){
                videoIndex = i;
                videoInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                continue;
            }
            if (MIME.startsWith("audio")){
                audioIndex = i;
                audioInputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            }
        }
    }


    private void initMediaMuxer(){
        try {
            muxer = new MediaMuxer(filePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (0 > videoTrackIndex){
            MediaFormat format = extractor.getTrackFormat(videoIndex);
            videoTrackIndex = muxer.addTrack(format);
        }
        if (0 > audioTrackIndex){
            MediaFormat format = extractor.getTrackFormat(audioIndex);
            audioTrackIndex = muxer.addTrack(format);
        }

    }

    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            if (!isMuxerStarted && 0 <= audioTrackIndex && 0 <= videoTrackIndex){
                muxer.start();
                isMuxerStarted = true;
            }else{
                throw new IllegalArgumentException("muxer track添加失败");
            }
            writeIntoBuffer(videoInputSize,videoTrackIndex,videoIndex);
            writeIntoBuffer(audioInputSize,audioTrackIndex,audioIndex);
            muxer.stop();
            muxer.release();
            extractor.release();
            Log.v("tag","released muxer and extractor");
        }
    });

    public void startDemuxAndMux(){
        thread.start();
    }


    private void writeIntoBuffer(int inputSize,int trackIndex , int index){
        MediaFormat format = extractor.getTrackFormat(index);
        String MIME = format.getString(MediaFormat.KEY_MIME);
        extractor.selectTrack(index);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while(true) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(inputSize);
            int size = extractor.readSampleData(inputBuffer , 0 );
            if (0 > size){
                Log.v("tag","start release Muxer And Extractor");
                break;
            }else{
                info.presentationTimeUs = extractor.getSampleTime();
                info.flags = extractor.getSampleFlags();
                info.offset = 0;
                info.size = size;
                muxer.writeSampleData(trackIndex,inputBuffer,info);
                extractor.advance();
                Log.d("tag","muxing " + MIME);
            }
        }
    }


}
