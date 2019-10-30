package com.example.myapplication3;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.example.myapplication3.TranscodeBuildUtils.Transcode;
import com.example.myapplication3.TranscodeBuildUtils.TranscodeBuilder;

import java.io.IOException;

/**
 * Created by weizheng.huang on 2019-10-30.
 */
public class TranscodeWrapperDemo2 {
    private int audioIndex = -1;
    private int videoIndex = -1;
    private double assignSizeRate = 1;
    private double durationTotal = 0;
    private String audioFormatType;
    private String videoFormatType;
    private String filePath;
    private AssetFileDescriptor srcFilePath;
    private AssetFileDescriptor srcFilePath2;
    private MediaExtractor extractor,audioExtractor;
    private MediaCodec decodec,encodec;
    private MediaCodec audioDecodec,audioEncodec;
    private MediaMuxer muxer;

    private Transcode transcode;



    ///////////public////////

    public void setAssignSize(double assignSizeRate) {
        this.assignSizeRate = assignSizeRate;
    }

    public void setPauseTranscode(boolean pauseTranscode) {
        transcode.setPauseTranscode(pauseTranscode);
    }
    public void setTailTime(long startTime ,long endTime){
        transcode.setTailTime(startTime,endTime);
    }

    public TranscodeWrapperDemo2(String filePath, AssetFileDescriptor srcFilePath, AssetFileDescriptor srcFilePath2) {
        this.filePath = filePath;
        this.srcFilePath = srcFilePath;
        this.srcFilePath2 = srcFilePath2;
    }


    public boolean startTranscode(){
        transcode.start();
        return true;
    }
    public  void init(){
        initMediaExtractor();
        initMediaCodec();
        initMediaMuxer();
        initTranscode();
    }

    ///////////private //////////////

    private void initTranscode(){
        TranscodeBuilder transcodeBuilder = new TranscodeBuilder();
        transcodeBuilder.buildAudioInputThread(audioExtractor,audioDecodec,audioEncodec,audioIndex,durationTotal);
        transcodeBuilder.buildVideoInputThread(extractor,decodec,encodec,videoIndex,durationTotal);
        transcodeBuilder.buildAudioOutputThread(muxer,audioEncodec,audioFormatType,durationTotal);
        transcodeBuilder.buildVideoOutputThread(muxer,encodec,videoFormatType,durationTotal);
        transcodeBuilder.setTIMEUS(50000l);
        transcode = transcodeBuilder.build();
    }
    private void initMediaExtractor(){
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(srcFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioExtractor = new MediaExtractor();
        try {
            audioExtractor.setDataSource(srcFilePath2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int frameRate;
    private int bitRate;
    private int width;
    private int height;
    private int audioBitRate;
    private int sampleRate;
    private int channelCount;

    private void initMediaCodec(){


        ////////decode///////
        for (int i = 0; i < extractor.getTrackCount(); i++){
            MediaFormat format = extractor.getTrackFormat(i);
            String formatType = format.getString(MediaFormat.KEY_MIME);

            if (formatType.startsWith("video")){
                videoIndex = i;
                try {
                    decodec = MediaCodec.createDecoderByType(formatType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoFormatType = formatType;
                frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                width = format.getInteger(MediaFormat.KEY_WIDTH);
                height = format.getInteger(MediaFormat.KEY_HEIGHT);
                durationTotal = format.getLong(MediaFormat.KEY_DURATION);
                decodec.configure(format,null,null,0);
                decodec.start();
                continue;
            }
            if (formatType.startsWith("audio")){
                audioIndex = i;
                try {
                    audioDecodec = MediaCodec.createDecoderByType(formatType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                audioFormatType = formatType;
                audioBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                audioDecodec.configure(format,null,null,0);
                audioDecodec.start();
            }
        }


        MediaFormat videoFormat = MediaFormat.createVideoFormat(videoFormatType, width, height);

        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,(int)(bitRate * assignSizeRate ));
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameRate * 2);

        MediaFormat audioFormat = MediaFormat.createAudioFormat(audioFormatType, sampleRate, channelCount);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE,(int)(audioBitRate * assignSizeRate ));




        try {
            encodec = MediaCodec.createEncoderByType(videoFormatType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        encodec.configure(videoFormat, null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        encodec.start();

        try {
            audioEncodec = MediaCodec.createEncoderByType(audioFormatType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioEncodec.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        audioEncodec.start();
    }
    private void initMediaMuxer(){

        try {
            muxer = new MediaMuxer(filePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
