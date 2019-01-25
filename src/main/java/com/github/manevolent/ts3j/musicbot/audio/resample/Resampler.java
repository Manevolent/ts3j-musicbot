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

    public int resampleFloats(float[] samples, AudioBuffer out) {
        return resampleFloats(samples, samples.length, out);
    }

    public int resampleDoubles(double[] sampleBuffer, AudioBuffer out) {
        return resampleDoubles(sampleBuffer, sampleBuffer.length, out);
    }


    public int resampleDoubles(double[] sampleBuffer, int samples, AudioBuffer out) {
        float[] floatSampleBuffer = new float[samples];
        for (int i = 0; i < samples; i ++) floatSampleBuffer[i] = (float) sampleBuffer[i];

        return resampleFloats(floatSampleBuffer, samples, out);
    }

    public int resampleFloats(float[] sampleBuffer, int samples, AudioBuffer out) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(samples * 4);
        byteBuffer.order(ByteOrder.nativeOrder());

        byteBuffer.asFloatBuffer().put(sampleBuffer, 0, samples);

        return resampleFloats(byteBuffer, samples, out);
    }

    public abstract int resamplePCM(byte[] frameData, int samples, AudioBuffer out);

    public abstract int resampleFloats(ByteBuffer floatBufferAsBytes, int samples, AudioBuffer out);

    public abstract int flush(AudioBuffer out);

    public AudioFormat getInputFormat() {
        return inputFormat;
    }
    public AudioFormat getOutputFormat() {
        return outputFormat;
    }
}
