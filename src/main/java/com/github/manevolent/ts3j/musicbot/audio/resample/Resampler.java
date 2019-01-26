package com.github.manevolent.ts3j.musicbot.audio.resample;

import com.github.manevolent.ts3j.musicbot.audio.AudioBuffer;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Resampler implements AutoCloseable {
    private final AudioFormat inputFormat, outputFormat;

    protected Resampler(AudioFormat inputFormat, AudioFormat outputFormat) {
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
    }

    public AudioFormat getInputFormat() {
        return inputFormat;
    }
    public AudioFormat getOutputFormat() {
        return outputFormat;
    }

    public int resample(AudioBuffer in, AudioBuffer out) {
        float[] samples = new float[in.availableOutput()];
        int read = in.read(samples, 0, samples.length);
        return resample(samples, read, out);
    }

    public int resample(float[] samples, AudioBuffer out) {
        return resample(samples, samples.length, out);
    }

    public int resample(double[] sampleBuffer, AudioBuffer out) {
        return resample(sampleBuffer, sampleBuffer.length, out);
    }

    public int resample(double[] sampleBuffer, int samples, AudioBuffer out) {
        float[] floatSampleBuffer = new float[samples];
        for (int i = 0; i < samples; i ++) floatSampleBuffer[i] = (float) sampleBuffer[i];

        return resample(floatSampleBuffer, samples, out);
    }

    public int resample(float[] sampleBuffer, int samples, AudioBuffer out) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(samples * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        byteBuffer.asFloatBuffer().put(sampleBuffer, 0, samples);

        return resample(byteBuffer, samples, out);
    }

    public abstract int resample(byte[] frameData, int samples, AudioBuffer out);
    public abstract int resample(ByteBuffer floatBufferAsBytes, int samples, AudioBuffer out);
    public abstract int flush(AudioBuffer out);
}
