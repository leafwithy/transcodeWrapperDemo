package com.example.myapplication3;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by weizheng.huang on 2019-10-15.
 */
public class EncoderMuxer {
    private MediaFormat audioFormat ;
    private MediaFormat videoFormat ;
    private MediaMuxer muxer;
    private MediaCodec audioEncodec;
    private MediaCodec videoEncodec;


    //    muxer封装后生成的文件的绝对路径
    private String videoMuxerPath  = Environment.getExternalStorageDirectory().getAbsolutePath() + "/shape.mp4";

//    muxer添加音视频格式后给出的信道
    private int videoTrack = -1;
    private int audioTrack = -1;

//    这里是HandlerThread的异步消息发送所做的准备
    private Handler mVideoEncodeHandler = null;
    private HandlerThread mVideoEncodeHandlerThread = new HandlerThread("VideoEncode");
    private Handler mAudioEncodeHandler = null;
    private HandlerThread mAudioEncodehandlerThread = new HandlerThread("AudioEncode");

    private MediaCodec.Callback videoCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            ByteBuffer inputBuffer =  videoEncodec.getInputBuffer(i);
            byte [] videoBytes = VideoDecoder.getVideoDataFromQueue();
            //    给出指定信道下音视频的每一帧的格式
            MediaCodec.BufferInfo videoBufferInfo = VideoDecoder.getBufferInfoFromQueue();
            if (videoBufferInfo != null) {
                inputBuffer.put(videoBytes);
                videoEncodec.queueInputBuffer(i,0, videoBufferInfo.size, videoBufferInfo.presentationTimeUs, videoBufferInfo.flags);
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = videoEncodec.getOutputBuffer(i);
            if (bufferInfo.size > 0) {
                muxer.writeSampleData(videoTrack, outputBuffer, bufferInfo);
                Log.d("tag","muxing video");
                videoEncodec.releaseOutputBuffer(i, false);

            }else{
                stopVideoEncode();
                releaseVideoEncode();
                muxer.stop();
                muxer.release();
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.v("tag","video encode error");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.v("tag","video encode format changed");
        }
    };

    private MediaCodec.Callback audioCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            ByteBuffer inputBuffer =  audioEncodec.getInputBuffer(i);
            byte [] audioBytes = AudioDecoder.getAudioDataFromQueue();
            MediaCodec.BufferInfo audioBufferInfo = AudioDecoder.getBufferInfoFromQueue();
            if (audioBufferInfo != null) {
                inputBuffer.put(audioBytes);
                audioEncodec.queueInputBuffer(i,0, audioBufferInfo.size, audioBufferInfo.presentationTimeUs,0);
            }
       }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = audioEncodec.getOutputBuffer(i);
            if (bufferInfo.size > 0 ) {
                muxer.writeSampleData(audioTrack, outputBuffer, bufferInfo);
                audioEncodec.releaseOutputBuffer(i,false);
                Log.d("tag","muxing audio");
            }else{
                stopAudioEncode();
                releaseAudioEncode();
                if (videoEncodec == null){
                    muxer.stop();
                    muxer.release();
                    Log.e("tag","muxer released");

                }
            }


        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.v("tag","audio encode error");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.v("tag","audio encode format changed");
        }
    };

    public EncoderMuxer(){
        try {
            muxer = new MediaMuxer(videoMuxerPath , MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        audioFormat = MediaFormat.createAudioFormat(FileConfig.AudioConfig.mimeType,FileConfig.AudioConfig.sampleRate, FileConfig.AudioConfig.chanelCount);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE,FileConfig.AudioConfig.bitRate);
        try {
            audioEncodec = MediaCodec.createEncoderByType(FileConfig.AudioConfig.mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }

        videoFormat = MediaFormat.createVideoFormat(FileConfig.VideoConfig.mimeType,FileConfig.VideoConfig.width,FileConfig.VideoConfig.height);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,FileConfig.VideoConfig.bitRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,FileConfig.VideoConfig.frameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,FileConfig.VideoConfig.iFrame_inteval);

        try {
            videoEncodec = MediaCodec.createEncoderByType(FileConfig.VideoConfig.mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoTrack = muxer.addTrack(videoFormat);
        audioTrack = muxer.addTrack(audioFormat);
        muxer.start();
        mVideoEncodeHandlerThread.start();
        mVideoEncodeHandler = new Handler(mVideoEncodeHandlerThread.getLooper());
        mAudioEncodehandlerThread.start();
        mAudioEncodeHandler = new Handler(mAudioEncodehandlerThread.getLooper());


    }

    public void startVideoEncode(){
        if (videoEncodec != null){
            Log.v("tag","start video encode");
            videoEncodec.setCallback(videoCallback,mVideoEncodeHandler);
            videoEncodec.configure(videoFormat,null,null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            videoEncodec.start();

        }else{
            throw new IllegalArgumentException("videoEncoder init wrong");
        }

    }

    public void startAudioEncode(){
        if (audioEncodec != null){
            Log.v("tag","start audio encode");
            audioEncodec.setCallback(audioCallback,mAudioEncodeHandler);
            audioEncodec.configure(audioFormat,null,null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            audioEncodec.start();
        }else{
            throw new IllegalArgumentException("audioEncode init wrong");
        }
    }


    private void stopVideoEncode(){
        if (videoEncodec != null){
            videoEncodec.stop();

            Log.e("tag","stoped videoEncode");
        }

    }
    private void stopAudioEncode(){
        if (audioEncodec != null){
            audioEncodec.stop();

            Log.e("tag","stoped audioEncode");
        }
    }

    private void releaseVideoEncode(){
        if (videoEncodec != null){
            videoEncodec.setCallback(null);
            videoEncodec.release();
            videoEncodec = null;
            Log.e("tag","videoEncode release");
        }
    }

    private void releaseAudioEncode(){
        if (audioEncodec != null){
            audioEncodec.setCallback(null);
            audioEncodec.release();
            audioEncodec = null;
            Log.e("tag","audioEncode release");
        }
    }
}
