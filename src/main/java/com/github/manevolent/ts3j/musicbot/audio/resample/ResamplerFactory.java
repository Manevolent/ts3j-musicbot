package com.github.manevolent.ts3j.musicbot.audio.resample;

import javax.sound.sampled.AudioFormat;

public interface ResamplerFactory {
    Resampler create(AudioFormat in, AudioFormat out, int bufferSize);
}
