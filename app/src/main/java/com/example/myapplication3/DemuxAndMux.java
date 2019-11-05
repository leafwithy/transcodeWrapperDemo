package com.example.myapplication3;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Created by weizheng.huang on 2019-10-31.
 */
public class DemuxAndMux {
    private Set<String> fileList;
    private MediaExtractor  extractor;
    private MediaMuxer muxer ;
    private String filePath;
    private int videoIndex  = -1;
    private int audioIndex = -1;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private boolean isMuxerStarted = false;
    private int videoInputSize;
    private int audioInputSize;
    private boolean isMuxered = false;

    public DemuxAndMux(Set<String> afd , String filePath){
        this.fileList = afd;
        this.filePath = filePath;
    }

    public void init(String srcFilePath){

        initMediaMuxer();
        initMediaExtractor(srcFilePath);
    }
    private void initMediaExtractor(String srcFilePath){
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
        if (0 > videoTrackIndex){
            MediaFormat format = extractor.getTrackFormat(videoIndex);
            videoTrackIndex = muxer.addTrack(format);
        }
        if (0 > audioTrackIndex){
            MediaFormat format = extractor.getTrackFormat(audioIndex);
            audioTrackIndex = muxer.addTrack(format);
        }
    }


    private void initMediaMuxer(){
        try {
            if (muxer == null)
            muxer = new MediaMuxer(filePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            long timeV = 0;
            long timeA = 0;
            for (String srcFilePath : fileList) {
                init(srcFilePath);
                ///////第一次启动muxer
                if (!isMuxerStarted) {
                    if (0 <= audioTrackIndex && 0 <= videoTrackIndex) {
                        muxer.start();
                        isMuxerStarted = true;
                    } else {
                        throw new IllegalArgumentException("muxer track添加失败");
                    }
                }
                timeV = writeIntoBuffer(videoInputSize, videoTrackIndex, videoIndex , timeV);
                timeA = writeIntoBuffer(audioInputSize, audioTrackIndex, audioIndex , timeA);
//                long time  = timeV < timeA ? timeV : timeA;
//                timeA = timeV = time;
                extractor.release();
            }
            muxer.stop();
            isMuxered = false;
            muxer.release();

            Log.v("tag","released muxer and extractor");
        }
    });

    public void startDemuxAndMux(){
        thread.start();
    }


    private long writeIntoBuffer(int inputSize,int trackIndex , int index , long time){
        MediaFormat format = extractor.getTrackFormat(index);
        String MIME = format.getString(MediaFormat.KEY_MIME);
        extractor.selectTrack(index);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        while(true) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(inputSize);
            int size = extractor.readSampleData(inputBuffer , 0 );
            if (0 > size){
                Log.v("tag","start release Muxer And Extractor");
                break;
            }else{
                info.presentationTimeUs = extractor.getSampleTime() + time;
                info.flags = extractor.getSampleFlags();
                info.offset = 0;
                info.size = size;
                muxer.writeSampleData(trackIndex,inputBuffer,info);
                extractor.advance();
                Log.d("tag","muxing " + MIME);
            }
        }
        return info.presentationTimeUs;
    }


}
