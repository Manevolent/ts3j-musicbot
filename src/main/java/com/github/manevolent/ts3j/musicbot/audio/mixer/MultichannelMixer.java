package com.github.manevolent.ts3j.musicbot.audio.mixer;

public class MultichannelMixer extends AbstractMixer {
    /**
     * Internal sample buffer
     */
    private final float[][] buffer;

    public MultichannelMixer(int bufferSize, float audioSampleRate, int audioChannels) {
        super(bufferSize, audioSampleRate, audioChannels);

        this.buffer = new float[audioChannels][bufferSize];
    }

    @Override
    public boolean processBuffer() {
        return false;
    }

    @Override
    public float getPositionInSeconds() {
        return 0;
    }
}
