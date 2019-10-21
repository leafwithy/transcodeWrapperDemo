package com.example.myapplication3;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
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
 * Created by weizheng.huang on 2019-10-14.
 */
public class VideoDecoder {
    private MediaExtractor extractor;
    private MediaCodec decodec ;
    public static MediaFormat format;
    private int size = -1;
    private File file = null;
    private byte [] bytes = null;
    private String videoFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/2.yuv";
    private FileOutputStream fos = null;

    private Handler mVideoDecoderHandler;
    private HandlerThread mVideoDecoderHandlerThread = new HandlerThread("VideoDecoder");

    private final static ArrayBlockingQueue<MediaCodec.BufferInfo> mBufferInfoFromQueue = new ArrayBlockingQueue<MediaCodec.BufferInfo>(10,true);
    private final static ArrayBlockingQueue<byte []> mVideoOutputABQueue = new ArrayBlockingQueue<byte[]>(10,true);

    private MediaCodec.Callback callback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
            ByteBuffer inputBuffer = decodec.getInputBuffer(i);
            inputBuffer.clear();
            size = extractor.readSampleData(inputBuffer,0);
            if (size >= 0 ) {
                decodec.queueInputBuffer(i, 0, size, extractor.getSampleTime(), extractor.getSampleFlags());
                extractor.advance();
            }

        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
            bytes  = new byte[bufferInfo.size];

            outputBuffer.get(bytes);
            try {
                mVideoOutputABQueue.put(bytes);

                mBufferInfoFromQueue.put(bufferInfo);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            outputBuffer.clear();
            decodec.releaseOutputBuffer(i,false);
            if (size < 0){
                stopDecoder();
                release();
            }

        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
            Log.v("tag","error decode video");
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
            Log.v("tag","format changed decode video");
        }

    };

    public VideoDecoder(AssetFileDescriptor filePath){
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < extractor.getTrackCount(); i++){
            format = extractor.getTrackFormat(i);
            String mimeType = format.getString(MediaFormat.KEY_MIME);
            if (mimeType.startsWith("video")){
                extractor.selectTrack(i);
                FileConfig.VideoConfig.mimeType = mimeType;
                FileConfig.VideoConfig.width = format.getInteger(MediaFormat.KEY_WIDTH);
                FileConfig.VideoConfig.height = format.getInteger(MediaFormat.KEY_HEIGHT);
                FileConfig.VideoConfig.frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                FileConfig.VideoConfig.bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
                FileConfig.length = FileConfig.VideoConfig.width * FileConfig.VideoConfig.height * 3;

                try {
                    decodec = MediaCodec.createDecoderByType(mimeType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.v("tag","decodec init correct");
                break;
            }
        }
        mVideoDecoderHandlerThread.start();
        mVideoDecoderHandler = new Handler(mVideoDecoderHandlerThread.getLooper());
    }

    public void startDecode(){
        if (decodec != null){
            Log.v("tag","start video decode");
            decodec.setCallback(callback,mVideoDecoderHandler);
            decodec.configure(format,null,null,0);
            decodec.start();
        }else{
            throw new IllegalArgumentException("videoDecoder init wrong");
        }
    }

    public void stopDecoder(){
        if (decodec != null){
            Log.d("tag","stoped");
            decodec.stop();
            decodec.setCallback(null);
        }
    }

    public void release(){
        if (decodec != null){
            Log.d("tag","released");
            extractor.release();
            decodec.release();
            decodec = null;
        }
    }

    public static byte [] getVideoDataFromQueue() {
        return mVideoOutputABQueue.poll();
    }
    public static MediaCodec.BufferInfo getBufferInfoFromQueue(){
            return mBufferInfoFromQueue.poll();
    }
}
