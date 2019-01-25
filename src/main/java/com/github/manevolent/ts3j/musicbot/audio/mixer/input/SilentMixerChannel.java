package com.github.manevolent.ts3j.musicbot.audio.mixer.input;

import java.io.IOException;

public class SilentMixerChannel implements MixerChannel {
    private final long start = System.nanoTime();
    private final float samplesPerSecond;
    private int channels;

    private long sent = 0L;
    private boolean closed = false;

    public SilentMixerChannel(float samplesPerSecond, int channels) {
        this.samplesPerSecond = samplesPerSecond;
        this.channels = channels;
    }

    @Override
    public String getName() {
        return samplesPerSecond + "Hz, " + channels + "ch Silence";
    }

    @Override
    public int available() {
        // Calculate the amount of time that has transpired since the channel was created
        long transpired = System.nanoTime() - start;

        // Calculate the amount of frames that should have been sent up to this point
        double samples = samplesPerSecond * ((double) transpired / 1_000_000_000D);

        // Calculate the amount of samples that should have been sent up to this point
        long transpiredSamples = ((long) Math.floor(samples)) * channels;

        // Return the amount of needed samples subtracted by the actual count sent
        return (int) (transpiredSamples - sent);
    }

    @Override
    public int read(float[] buffer, int len) throws IOException {
        // Get the count of samples needed to be sent
        int available = available();

        // Calculate the count of samples to "send"
        int copy = Math.min(available, len);

        // Advance the position of the silence generator
        sent += copy;

        // Return the silent samples
        return copy;
    }

    @Override
    public boolean isPlaying() {
        return !closed;
    }

    @Override
    public void close() throws Exception {
        closed = true;
    }
}
