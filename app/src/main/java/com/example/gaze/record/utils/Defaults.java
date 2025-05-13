package com.example.gaze.record.utils;

import android.annotation.SuppressLint;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.core.VideoCapture.Builder;

@SuppressLint("RestrictedApi")
public final class Defaults
        implements ConfigProvider<VideoCaptureConfig> {
    private final int DEFAULT_VIDEO_FRAME_RATE = 30;
    /**
     * 8Mb/s the recommend rate for 30fps 1080p
     */
    private final int DEFAULT_BIT_RATE = 8 * 1024 * 1024;
    /**
     * Seconds between each key frame
     */
    private final int DEFAULT_INTRA_FRAME_INTERVAL = 1;
    /**
     * audio bit rate
     */
    private final int DEFAULT_AUDIO_BIT_RATE = 64000;
    /**
     * audio sample rate
     */
    private final int DEFAULT_AUDIO_SAMPLE_RATE = 8000;
    /**
     * audio channel count
     */
    private final int DEFAULT_AUDIO_CHANNEL_COUNT = 1;
    /**
     * audio default minimum buffer size
     */
    private final int DEFAULT_AUDIO_MIN_BUFFER_SIZE = 1024;
    /**
     * Current max resolution of VideoCapture is set as FHD
     */
    private final Size DEFAULT_MAX_RESOLUTION = new Size(480, 640);
    /**
     * Surface occupancy priority to this use case
     */
    private final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 3;
    private final int DEFAULT_ASPECT_RATIO = AspectRatio.RATIO_4_3;

    private final VideoCaptureConfig DEFAULT_CONFIG;

    public Defaults(){
        Builder builder = new Builder()
                .setVideoFrameRate(DEFAULT_VIDEO_FRAME_RATE)
                .setBitRate(DEFAULT_BIT_RATE)
                .setIFrameInterval(DEFAULT_INTRA_FRAME_INTERVAL)
                .setAudioBitRate(DEFAULT_AUDIO_BIT_RATE)
                .setAudioSampleRate(DEFAULT_AUDIO_SAMPLE_RATE)
                .setAudioChannelCount(DEFAULT_AUDIO_CHANNEL_COUNT)
                .setAudioMinBufferSize(DEFAULT_AUDIO_MIN_BUFFER_SIZE)
                .setMaxResolution(DEFAULT_MAX_RESOLUTION)
                .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                .setTargetAspectRatio(DEFAULT_ASPECT_RATIO);

        DEFAULT_CONFIG = builder.getUseCaseConfig();
    }

    @NonNull
    @Override
    public VideoCaptureConfig getConfig() {
        return DEFAULT_CONFIG;
    }
}