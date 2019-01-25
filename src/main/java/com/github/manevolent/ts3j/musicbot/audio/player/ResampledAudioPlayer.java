package com.github.manevolent.ts3j.musicbot.audio.player;

import com.github.manevolent.ts3j.musicbot.audio.resample.Resampler;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;

public class ResampledAudioPlayer extends BufferedAudioPlayer {
    private final AudioPlayer player;
    private final Resampler resampler;

    public ResampledAudioPlayer(AudioPlayer player, Resampler resampler, int bufferSize) {
        super(bufferSize);

        this.player = player;
        this.resampler = resampler;
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
    public boolean processBuffer() throws IOException {
        return false;
    }

    @Override
    public void close() throws Exception {
        player.close();
    }

    public AudioFormat getFormat() {
        return resampler.getOutputFormat();
    }
}
