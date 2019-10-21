package com.example.myapplication3;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by weizheng.huang on 2019-10-15.
 */
public class AudioDecoder {
    private MediaExtractor extractor;
    private MediaCodec codec;
    public static MediaFormat format;
    private String formatType;
    private File file = null;
    private FileOutputStream fos = null;


    private int size = -1;
    private Handler mAudioHandler ;
    private HandlerThread mAudioDecodeHandlerThread = new HandlerThread("AudioDecoder");

    private final static ArrayBlockingQueue<MediaCodec.BufferInfo> mBufferInfoOutputABQueue = new ArrayBlockingQueue<MediaCodec.BufferInfo>(10,true);
    private final static ArrayBlockingQueue<byte []> mAudioOutputABQueue = new ArrayBlockingQueue<byte []>(10,true);


    private MediaCodec.Callback callback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            ByteBuffer inputBuffer = codec.getInputBuffer(i);
            inputBuffer.clear();
            size = extractor.readSampleData(inputBuffer,0);
            if (size >= 0) {
                codec.queueInputBuffer(i, 0, size, extractor.getSampleTime(), extractor.getSampleFlags());
                extractor.advance();
            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = codec.getOutputBuffer(i);
            byte[] bytes = new byte[bufferInfo.size];
            outputBuffer.get(bytes);
            try {
                mAudioOutputABQueue.put(bytes);
                mBufferInfoOutputABQueue.put(bufferInfo);

            }catch (InterruptedException e){
                e.printStackTrace();
            }
            outputBuffer.clear();
            codec.releaseOutputBuffer(i,false);
            if (size < 0){

                stopDecoder();
                release();
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.d("tag","error decode audio");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.d("tag","format changed audio");
        }
    };

    public AudioDecoder(AssetFileDescriptor filePath){
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            format = extractor.getTrackFormat(i);
            formatType = format.getString(MediaFormat.KEY_MIME);
            if (formatType.startsWith("audio")) {
                extractor.selectTrack(i);
                FileConfig.AudioConfig.bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                FileConfig.AudioConfig.sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                FileConfig.AudioConfig.chanelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                FileConfig.AudioConfig.mimeType = formatType;
                try {
                    codec = MediaCodec.createDecoderByType(formatType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        mAudioDecodeHandlerThread.start();
        mAudioHandler = new Handler(mAudioDecodeHandlerThread.getLooper());
    }
    public static byte[] getAudioDataFromQueue() {
        return mAudioOutputABQueue.poll();
    }

    public static MediaCodec.BufferInfo getBufferInfoFromQueue(){
        return mBufferInfoOutputABQueue.poll();
    }

    public void startDecode(){
        if (codec != null ){
            Log.v("tag","start audio decode");
            codec.setCallback(callback,mAudioHandler);
            codec.configure(format,null,null,0);
            codec.start();
        }else{
            throw new IllegalArgumentException("startDecoder failed , decodec forget to be initialed");
        }
    }


    private void stopDecoder(){
        if (codec != null){
            codec.stop();
            codec.setCallback(null);
        }
    }


    private void release(){
        if (codec != null){
            codec.release();
            extractor.release();
            codec = null;
        }
    }
}
