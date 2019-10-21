package com.example.myapplication3;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by weizheng.huang on 2019-10-18.
 */
public class TranscodeWrapperDemo {
    /////////private  //////

    private MediaExtractor extractor;

    private MediaCodec decodec,encodec,audioDecodec,audioEncodec;

    private MediaMuxer muxer;

    private MediaFormat videoFormat;
    private MediaFormat audioFormat;
    private String filePath = null;
    private AssetFileDescriptor srcFilePath = null;

    private int audioTrackIndex = -1 ;
    private int videoTrackIndex = -1 ;
    private int videoIndex  = -1;
    private int audioIndex = -1;

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
            try {
                Thread.sleep(1000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            extractor.release();
            decodec.stop();
            decodec.release();
            audioDecodec.stop();
            audioDecodec.release();
            Log.d("tag","released decode");
        }
    });

//    private Thread audioInputThread = new Thread(new Runnable() {
//        @Override
//        public void run() {
//            audioInputLoop();
//            audioExtractor.release();
//            audioDecodec.stop();
//            audioDecodec.release();
//        }
//    });
    private Thread outputThread = new Thread(new Runnable() {
        @Override
        public void run() {

            outputLoop();

            try {
                Thread.sleep(1000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            encodec.stop();
            encodec.release();
            audioEncodec.stop();
            audioEncodec.release();
            Log.d("tag","released encode");
            muxer.stop();
            muxer.release();
            Log.d("tag","released muxer");
        }
    });

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

        videoFormat = MediaFormat.createVideoFormat(videoFormatType,width,height);

        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,bitRate);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,iFrameInteval);

        audioFormat = MediaFormat.createAudioFormat(audioFormatType,sampleRate,channelCount);

        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE,audioBitRate);



        try {
            encodec = MediaCodec.createEncoderByType(videoFormatType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        encodec.configure(videoFormat , null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
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
        ////muxer添加track的，生成的第一帧里含有csd信息
        videoTrackIndex = muxer.addTrack(videoFormat);

        audioTrackIndex = muxer.addTrack(audioFormat);
        muxer.start();

    }

    private void inputLoop() {
//        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +"/4.yuv");
//        while(fos == null) {
//            try {
//                fos = new FileOutputStream(file);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }

        //////////video decode///////////////
        extractor.selectTrack(videoIndex);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while(true) {
            int inputIndex = decodec.dequeueInputBuffer(10000l);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = decodec.getInputBuffer(inputIndex);
                int size = extractor.readSampleData(inputBuffer, 0);
                if (size >= 0) {
      //              Log.d("tag","video decode...");
                    decodec.queueInputBuffer(inputIndex, 0, size, extractor.getSampleTime(), extractor.getSampleFlags());
                    extractor.advance();

                } else {
                    decodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                }
            }

            int outputIndex = decodec.dequeueOutputBuffer(info, 10000l);
            if (outputIndex >= 0){
                    ByteBuffer outputBuffer = decodec.getOutputBuffer(outputIndex);

                    int inputBufferEncodeIndex = encodec.dequeueInputBuffer(10000l);
                    if (inputBufferEncodeIndex >= 0) {
                        ByteBuffer inputEncodeBuffer = encodec.getInputBuffer(inputBufferEncodeIndex);
                        if (info.size >= 0) {
                            //              Log.d("tag","into video encode...");
                            inputEncodeBuffer.put(outputBuffer);
                            encodec.queueInputBuffer(inputBufferEncodeIndex, 0, info.size, info.presentationTimeUs, info.flags);
                        } else {
                            encodec.queueInputBuffer(inputBufferEncodeIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                    }
                    decodec.releaseOutputBuffer(outputIndex, false);
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("tag","start release Decode");
                break;
            }
        }




        /////////audio decode////////////
        extractor.selectTrack(audioIndex);
        MediaCodec.BufferInfo mediaInfo = new MediaCodec.BufferInfo();
        while(true) {
            int inputIndex = audioDecodec.dequeueInputBuffer(10000l);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = audioDecodec.getInputBuffer(inputIndex);
                int size = extractor.readSampleData(inputBuffer, 0);
                if (size >= 0) {
         //           Log.d("tag","audio decode...");
                    audioDecodec.queueInputBuffer(inputIndex, 0, size, extractor.getSampleTime(), extractor.getSampleFlags());
                    extractor.advance();

                } else {
                    audioDecodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                }
            }

            int outputIndex = audioDecodec.dequeueOutputBuffer(mediaInfo, 10000l);
            if (outputIndex >= 0){
                    ByteBuffer outputBuffer = audioDecodec.getOutputBuffer(outputIndex);
                    int inputBufferEncodeIndex = audioEncodec.dequeueInputBuffer(10000l);
                    if (inputBufferEncodeIndex >= 0) {
                        ByteBuffer inputEncodeBuffer = audioEncodec.getInputBuffer(inputBufferEncodeIndex);
                        if (mediaInfo.size >= 0) {
                            //                Log.d("tag","into audio encode...");
                            inputEncodeBuffer.put(outputBuffer);
                            audioEncodec.queueInputBuffer(inputBufferEncodeIndex, 0, mediaInfo.size, mediaInfo.presentationTimeUs, mediaInfo.flags);
                        } else {
                            audioEncodec.queueInputBuffer(inputBufferEncodeIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                    }
                    audioDecodec.releaseOutputBuffer(outputIndex, false);
            }
            if ((mediaInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("tag","start release Encode");
                break;
            }
        }
    }

//    private void audioInputLoop(){
//
//        audioExtractor.selectTrack(audioIndex);
//        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//        while(true) {
//            int inputIndex = audioDecodec.dequeueInputBuffer(10000l);
//            if (inputIndex >= 0) {
//                ByteBuffer inputBuffer = audioDecodec.getInputBuffer(inputIndex);
//                int size = audioExtractor.readSampleData(inputBuffer, 0);
//                if (size >= 0) {
//                    Log.d("tag","audio decode...");
//                    audioDecodec.queueInputBuffer(inputIndex, 0, size, audioExtractor.getSampleTime(), audioExtractor.getSampleFlags());
//                    audioExtractor.advance();
//
//                } else {
//                    audioDecodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//
//                }
//            }
//
//            int outputIndex = audioDecodec.dequeueOutputBuffer(info, 10000l);
//            if (outputIndex >= 0) {
//                ByteBuffer outputBuffer = audioDecodec.getOutputBuffer(outputIndex);
//                int inputBufferEncodeIndex = audioDecodec.dequeueInputBuffer(10000l);
//                if (inputBufferEncodeIndex >= 0) {
//                    ByteBuffer inputEncodeBuffer = audioDecodec.getInputBuffer(inputBufferEncodeIndex);
//                    if (info.size >= 0) {
//                        Log.d("tag","into audio encode...");
//                        inputEncodeBuffer.put(outputBuffer);
//                        audioEncodec.queueInputBuffer(inputBufferEncodeIndex, 0, info.size, info.presentationTimeUs, info.flags);
//
//                    }else{
//                        audioEncodec.queueInputBuffer(inputBufferEncodeIndex,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                    }
//                }
//                audioDecodec.releaseOutputBuffer(outputIndex, false);
//            }
//            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//                Log.d("tag","start release Encode");
//                break;
//            }
//        }
//    }

    private void outputLoop(){
        ////////////video encode/////////////////////
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
       // MediaCodec.BufferInfo mediaInfo = new MediaCodec.BufferInfo();
        while(true){
            int outputBufferIndex = encodec.dequeueOutputBuffer(info,10000l);
            if (outputBufferIndex >= 0){
               // mediaInfo.flags = mediaInfo
                ByteBuffer outputBuffer = encodec.getOutputBuffer(outputBufferIndex);
                if (info.size >= 0) {

                    muxer.writeSampleData(videoTrackIndex, outputBuffer, info);
                    //     Log.d("tag","video muxing");
                }
                encodec.releaseOutputBuffer(outputBufferIndex,false);
            }
            if ((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){

                Log.d("tag","start release video Encode");
                break;
            }
        }


        //////////////audio encode/////////////
        MediaCodec.BufferInfo mediaInfo = new MediaCodec.BufferInfo();

        while(true){
            int outputBufferIndex = audioEncodec.dequeueOutputBuffer(mediaInfo,10000l);
            if (outputBufferIndex >= 0){
                // mediaInfo.flags = mediaInfo
                ByteBuffer outputBuffer = audioEncodec.getOutputBuffer(outputBufferIndex);
                if (mediaInfo.size >= 0 ) {

                    muxer.writeSampleData(audioTrackIndex, outputBuffer, mediaInfo);
                    //    Log.d("tag","audio muxing");
                }
                audioEncodec.releaseOutputBuffer(outputBufferIndex,false);
            }
            if ((mediaInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){

                Log.d("tag","start release audio Encode");
                break;
            }
        }
    }

//    private void audioOutputLoop(){
//        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//        // MediaCodec.BufferInfo mediaInfo = new MediaCodec.BufferInfo();
//        while(true){
//            int outputBufferIndex = audioEncodec.dequeueOutputBuffer(info,10000l);
//            if (outputBufferIndex >= 0){
//                // mediaInfo.flags = mediaInfo
//                ByteBuffer outputBuffer = audioEncodec.getOutputBuffer(outputBufferIndex);
//                muxer.writeSampleData(audioTrackIndex, outputBuffer, info);
//                Log.d("tag","audio muxing");
//                audioEncodec.releaseOutputBuffer(outputBufferIndex,false);
//            }
//            if ((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
//
//                Log.d("tag","start release audio Encode");
//                break;
//            }
//        }
//    }


    /////////public //////////
    public void startTranscode(){

        inputThread.start();
        outputThread.start();

    }






}
