package com.github.manevolent.ts3j.musicbot.audio.mixer;

import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.MixerFilter;
import com.github.manevolent.ts3j.musicbot.audio.mixer.input.MixerChannel;
import com.github.manevolent.ts3j.musicbot.audio.mixer.output.MixerSink;

import java.util.Collection;
import java.util.List;

/**
 * Represents the scaffolding the mixer system in the bot.
 *
 * Channel - audio input (i.e. YouTube video or MP3).  Mixed simultaneously.
 * Sink - audio output target (i.e. Teamspeak)
 * Filter - mixer filters are used to morph data (i.e. clip protection, EQs, stuff)
 */
public interface Mixer {

    /**
     * Gets an immutable list of the sinks in the mixer.
     * @return Mixer sinks
     */
    Collection<MixerSink> getSinks();
    boolean addSink(MixerSink sink);
    boolean removeSink(MixerSink sink);

    /**
     * Gets an immutable list of channels in the mixer.
     * @return Mixer channels
     */
    Collection<MixerChannel> getChannels();
    boolean addChannel(MixerChannel channel);
    boolean removeChannel(MixerChannel channel);

    /**
     * Gets an immutable list of filters in the mixer.
     * @return Mixer filters
     */
    Collection<List<MixerFilter>> getFilters();
    List<MixerFilter> addFilter(MixerFilter... channel);
    boolean removeFilter(List<MixerFilter> filterList);

    boolean setRunning(boolean running);

    /**
     * Finds if the mixer is running, false othwerise.
     * @return true if running, false otherwise.
     */
    boolean isRunning();

    /**
     * Finds if the mixer should be playing, based on its contained channels.
     * @return true if playing, false otherwise.
     */
    boolean isPlaying();

    /**
     * Finds the number of available samples on the mixer.
     * @return Available samples on the mixer.
     */
    int available();

    /**
     * Empties the mixer.
     */
    void empty();

    /**
     * Processes the mixer buffer.
     * @return true if there are more processes to execute, false otherwise.
     */
    boolean processBuffer();

    /**
     * Gets the buffer size of the mixer, in samples.
     * @return Mixer buffer size, in samples.
     */
    int getBufferSize();

    /**
     * Gets the position of the mixer, in seconds.
     * @return Mixer position in seconds.
     */
    float getPositionInSeconds();

    /**
     * Gets the sample rate of this mixer, which all sinks and channels must conform to.
     * @return Sample rate
     */
    float getAudioSampleRate();

    /**
     * Gets the audio channel count of this mixer, which all sinks and channels must confirm to.
     * @return Channel count.
     */
    int getAudioChannels();

}
