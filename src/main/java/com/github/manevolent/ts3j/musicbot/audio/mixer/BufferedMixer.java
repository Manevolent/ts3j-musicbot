package com.github.manevolent.ts3j.musicbot.audio.mixer;

import com.github.manevolent.ts3j.musicbot.audio.mixer.input.MixerChannel;
import com.github.manevolent.ts3j.musicbot.audio.mixer.output.MixerSink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class BufferedMixer extends AbstractMixer {
    /**
     * Internal sample buffer
     */
    private final float[] buffer, mixBuffer;

    private long position = 0L;

    public BufferedMixer(int bufferSize, float audioSampleRate, int audioChannels) {
        super(bufferSize, audioSampleRate, audioChannels);

        this.buffer = new float[bufferSize];
        this.mixBuffer = new float[bufferSize];
    }

    @Override
    public boolean processBuffer() {
            if (!isPlaying())
                return false;

            // Plan for mixer input, ensuring the available sample count doesn't overflow the mixer
            int len = Math.min(buffer.length, available());

            // If no samples are ready, we don't bother with this, but we do signal that we must
            // continue playing samples.
            if (len > 0) {
                if (len > buffer.length)
                    throw new ArrayIndexOutOfBoundsException(len + " > " + buffer.length);

                // Reset buffers
                for (int i = 0; i < len; i++) buffer[i] = 0f;

                // Find if the system is in mixed-priority mode
                Iterator<MixerChannel> channelIterator = getChannels().iterator();
                MixerChannel channel;
                while (channelIterator.hasNext()) {
                    channel = channelIterator.next();
                    if (channel == null) continue;

                    try {
                        // Remove if the player is complete, otherwise mix
                        if (!channel.isPlaying()) {
                            removeChannel(channel);
                        } else {
                            // Reset mixing buffer
                            for (int i = 0; i < len; i++) mixBuffer[i] = 0f;

                            // Read samples from channel
                            int read = channel.read(mixBuffer, 0, len);

                            // Perform actual mixing
                            for (int i = 0; i < read; i ++)
                                buffer[i] += mixBuffer[i];
                        }
                    } catch (Throwable e) {
                        Logger.getGlobal().log(Level.SEVERE, "Problem playing audio on channel", e);
                        removeChannel(channel);
                    }
                }

                // Calculate samples per channel
                int smpPerCh = len / getAudioChannels();

                // Write to sinks
                for (MixerSink sink : getSinks().stream().filter(MixerSink::isRunning).collect(Collectors.toList()))
                    sink.write(buffer, len);

                position += len;
            }

            // Kill the mixer, ensure it stops if necessary after we've processed all the buffers/channels
            getChannels().stream().filter(x -> !x.isPlaying()).forEach(this::removeChannel);

            // Find if the mixer is still playing
            return isPlaying();
    }

    @Override
    public float getPositionInSeconds() {
        return (float)position / (float)(getAudioChannels() * getAudioSampleRate());
    }
}
