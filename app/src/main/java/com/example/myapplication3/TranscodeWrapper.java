//package com.example.myapplication3;
//
//import android.content.Context;
//import android.media.MediaCodec;
//import android.media.MediaExtractor;
//import android.media.MediaMuxer;
//
//import java.nio.ByteBuffer;
//import java.util.List;
//
///**
// * Created by weizheng.huang on 2019-10-18.
// */
//public class TranscodeWrapper {
//
//    private Context mContext;
//    private MediaExtractor mediaExtractor;
//    private MediaCodec mediaCodecDecoder;
//    private MediaCodec mediaCodecEncoder;
//    private MediaMuxer mediaMuxer;
//
//    private Thread inputThread, outputThread;
//
//    public int TranscodeWrapper(Context context) {
//        mContext = context;
//
//        initExtractor();
//        initDecoder();
//        initEncoder();
//
//        inputThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    int res = inputLoop();
//                    if (0 > res)
//                        return;
//                }
//            }
//        });
//
//        outputThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int res = outputLoop();
//                if (0 > res)
//                    return;
//            }
//        });
//
//        return 0;
//    }
//
//    public int startTranscode() {
//        if (null != inputThread)
//            inputThread.start();
//        if (null != outputThread)
//            outputThread.start();
//
//        return 0;
//    }
//
//    public int release() {
//        return 0;
//    }
//
//    /////////// Private ////////
//
//    private int initExtractor() {
//        return 0;
//    }
//
//    private int initDecoder() {
//        return 0;
//    }
//
//    private int initEncoder() {
//        return 0;
//    }
//
//
//    private int inputLoop() {
//        ByteBuffer demuxData = mediaExtractor.readSampleData();
//
//        List<ByteBuffer> deocderInputBufferList;
//        deocderInputBufferList = mediaCodecDecoder.getInputBuffers();
//        ByteBuffer deocderInputBuffer;
//
//        if (END_OF_STREAM) {
//            mediaCodecDecoder.queueInputBuffer(EOF);
//            return EOF;
//        } else {
//            while (true) {
//                int index = mediaCodecDecoder.dequeueInputBuffer(20);
//                if (0 > index)
//                    continue;
//
//                deocderInputBuffer = deocderInputBufferList[index];
//
//                demuxData-- > deocderInputBuffer;
//
//                mediaCodecDecoder.queueInputBuffer(index, .....);
//
//                break;
//            }
//        }
//        if (error)
//            return -1;
//
//        return 0;
//    }
//
//    private boolean decoderEOF =false;
//
//    private int outputLoop() {
//        ByteBuffer decoderOutputData;
//        if (false == decoderEOF) {
//            while (true) {
//                int index = mediaCodecDecoder.dequeueOutputBuffer(...);
//                if (index == EOF)
//                    decoderEOF = ture;
//                else if (0 > index)
//                    continue;
//
//                decoderOutputData = decoderOutputDataList[index];
//                break;
//            }
//
//
//            while (true) {
//                int index = mediaCodecEncoder.dequeueInputBuffer(20);
//                if (0 > index)
//                    continue;
//
//                encoderBuffer = encoderBufferList[index];
//                decoderOutputData--->encoderBuffer;
//
//                mediaCodecEncoder.queueInputBuffer(....);
//
//            }
//        } else {
//            mediaCodecEncoder.queueInputBuffer(MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//        }
//
//        int index = mediaCodecEncoder.dequeueOutputBuffer();
//        encoderOutputBuffer = encoderOutputBufferList[index];
//        MediaCodec.BufferInfo bufferInfo;
//
//        mediaMuxer.writeSampleData(index, encoderOutputBuffer, bufferInfo);
//
//        if (ENCODER_EOF) {
//            mediaMuxer.stop();
//            return -1;
//        }
//
//        return 0;
//    }
//}
