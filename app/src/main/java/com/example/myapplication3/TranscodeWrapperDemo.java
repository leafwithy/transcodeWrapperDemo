package com.example.myapplication3;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by weizheng.huang on 2019-10-18.
 */
public class TranscodeWrapperDemo {
    /////////private  //////

    private MediaExtractor extractor;

    private MediaCodec decodec,encodec;

    private MediaMuxer muxer;

    private MediaFormat videoFormat;
    private MediaFormat audioFormat;
    private String filePath = null;
    private AssetFileDescriptor srcFilePath = null;

    private int audioTrackIndex = -1 ;
    private int videoTrackIndex = -1 ;
    private int audioIndex = -1;


    private File file ;
    private FileOutputStream fos ;

    public TranscodeWrapperDemo(String filePath, AssetFileDescriptor srcFilePath) {
        this.filePath = filePath;
        this.srcFilePath = srcFilePath;
        initMediaExtractor();
        initMediaCodec();
        initMediaMuxer();
    }

    private Thread inputThread = new Thread(new Runnable() {
        @Override
        public void run() {
            inputLoop();
            extractor.release();
            decodec.stop();
            decodec.release();

        }
    });

//    private Thread outputThread = new Thread(new Runnable() {
//        @Override
//        public void run() {
//
//            outputLoop();
//            encodec.stop();
//            encodec.release();
//            muxer.stop();
//            muxer.release();
//        }
//    });

    private void initMediaExtractor(){
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(srcFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String videoFormatType = null;
    private String audioFormatType = null;
    private int width = -1;
    private int height = -1;
    private int frameRate = -1 ;
    private int bitRate = -1;
    private int iFrameInteval = 10;
    private int audioBitRate = -1 ;
    private int sampleRate = -1;
    private int channelCount = -1;

    private void initMediaCodec(){


        ////////decode///////
        for (int i = 0; i < extractor.getTrackCount(); i++){
            MediaFormat format = extractor.getTrackFormat(i);
            String formatType = format.getString(MediaFormat.KEY_MIME);

            if (formatType.startsWith("video")){
                extractor.selectTrack(i);
                try {
                    decodec = MediaCodec.createDecoderByType(formatType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoFormatType = formatType;
                width = format.getInteger(MediaFormat.KEY_WIDTH);
                height = format.getInteger(MediaFormat.KEY_HEIGHT);
                frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);

                decodec.configure(format,null,null,0);
                decodec.start();


                break;
            }
//            if (formatType.startsWith("audio")){
//                extractor.selectTrack(i);
//
//                audioFormatType = formatType;
//                audioBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
//                channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
//                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
//            }
        }
        ///////encode////////
        videoFormat = MediaFormat.createVideoFormat(videoFormatType,width,height);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,bitRate);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,iFrameInteval);

//        audioFormat = MediaFormat.createAudioFormat(audioFormatType,sampleRate,channelCount);
//        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE,audioBitRate);

//        try {
//            encodec = MediaCodec.createEncoderByType(videoFormatType);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        encodec.configure(videoFormat , null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
//        encodec.start();
    }

    private void initMediaMuxer(){

        try {
            muxer = new MediaMuxer(filePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (videoFormat != null && muxer != null)
        videoTrackIndex = muxer.addTrack(videoFormat);
        //audioTrackIndex = muxer.addTrack(audioFormat);
        muxer.start();
    }

    private int inputLoop() {
        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/4.yuv");
        while(fos == null) {
            try {
                fos = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        while(true) {
            int inputIndex = decodec.dequeueInputBuffer(10000l);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = decodec.getInputBuffer(inputIndex);

                int size = extractor.readSampleData(inputBuffer, 0);
                if (size >= 0) {
                    decodec.queueInputBuffer(inputIndex, 0, size, extractor.getSampleTime(), extractor.getSampleFlags());
                    extractor.advance();

                } else {
                    decodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                }
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputIndex = decodec.dequeueOutputBuffer(info, 10000l);
            if (outputIndex >= 0) {
                ByteBuffer outputBuffer = decodec.getOutputBuffer(outputIndex);
                  Log.d("tag","writing...") ;

                byte[] bytes = new byte[info.size];
                outputBuffer.get(bytes);
                try {
                    fos.write(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                int inputBufferEncodeIndex = encodec.dequeueInputBuffer(10000l);
//                if (inputBufferEncodeIndex >= 0) {
//                    if (info.size > 0)
//                    encodec.queueInputBuffer(inputBufferEncodeIndex,0, info.size,info.presentationTimeUs,0);
//                }
                decodec.releaseOutputBuffer(outputIndex, false);
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d("tag","start release");
                return -1;
            }
        }
    }

//    private int outputLoop(){
//        while(true){
//            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//            int outputBufferIndex = encodec.dequeueOutputBuffer(info,10000l);
//            if (outputBufferIndex >= 0){
//                ByteBuffer outputBuffer = encodec.getOutputBuffer(outputBufferIndex);
//                if (info.size > 0 ) {
//                    muxer.writeSampleData(videoTrackIndex, outputBuffer, info);
//                    Log.d("tag","muxing");
//                }else{
//                    encodec.stop();
//                    encodec.release();
//                }
//            }
//        }
//
//    }

    /////////public //////////
    public void startTranscode(){

        inputThread.start();
    //    outputThread.start();
    }






}
