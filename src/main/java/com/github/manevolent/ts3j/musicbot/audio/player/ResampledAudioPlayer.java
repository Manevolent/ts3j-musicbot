package com.github.manevolent.ts3j.musicbot.audio.player;

import com.github.manevolent.ts3j.musicbot.audio.AudioBuffer;
import com.github.manevolent.ts3j.musicbot.audio.resample.Resampler;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;

public class ResampledAudioPlayer extends BufferedAudioPlayer {
    private final AudioPlayer player;
    private final Resampler resampler;
    private final AudioBuffer resampleBuffer;

    private boolean closed = false;

    public ResampledAudioPlayer(AudioPlayer player, Resampler resampler, int bufferSize) {
        super(bufferSize);

        this.player = player;
        this.resampler = resampler;

        this.resampleBuffer = new AudioBuffer(bufferSize);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isPlaying() {
        return player.isPlaying();
    }

    @Override
    public boolean stop() {
        boolean stopped = player.stop();

        if (stopped) resampler.flush(getBuffer());

        return stopped;
    }

    @Override
    protected boolean processBuffer() throws IOException {
        int available = Math.min(getBuffer().availableInput(), player.available());

        // Read samples from the player into the buffer
        int read = resampleBuffer.write(player, available);

        // EOF check
        if (read <= 0) return false;

        // Resample the retrieved samples into the resample buffer
        int resampled = resampler.resample(resampleBuffer, getBuffer());

        return isPlaying();
    }

    @Override
    public void close() throws Exception {
        player.close();
        resampler.close();

        closed = true;
    }

    public AudioFormat getFormat() {
        return resampler.getOutputFormat();
    }

    @Override
    public int getSampleRate() {
        return (int) resampler.getOutputFormat().getSampleRate();
    }

    @Override
    public int getChannels() {
        return resampler.getOutputFormat().getChannels();
    }
}
