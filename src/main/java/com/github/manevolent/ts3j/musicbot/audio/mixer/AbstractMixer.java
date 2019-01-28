package com.github.manevolent.ts3j.musicbot.audio.mixer;

import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.MixerFilter;
import com.github.manevolent.ts3j.musicbot.audio.mixer.input.AudioProvider;
import com.github.manevolent.ts3j.musicbot.audio.mixer.input.MixerChannel;
import com.github.manevolent.ts3j.musicbot.audio.mixer.output.MixerSink;
import com.github.manevolent.ts3j.musicbot.audio.player.ResampledAudioPlayer;

import java.util.*;
import java.util.logging.Logger;

public abstract class AbstractMixer implements Mixer {
    private final int bufferSize;

    private final float audioSampleRate;
    private final int audioChannels;

    private final List<MixerSink> sinks = Collections.synchronizedList(new LinkedList<>());
    private final List<MixerChannel> channels = Collections.synchronizedList(new LinkedList<>());
    private final List<List<MixerFilter>> filters = Collections.synchronizedList(new LinkedList<>());

    private final Object channelLock = new Object();

    public AbstractMixer(int bufferSize, float audioSampleRate, int audioChannels) {
        this.bufferSize = bufferSize;

        this.audioSampleRate = audioSampleRate;
        this.audioChannels = audioChannels;
    }

    @Override
    public Collection<MixerSink> getSinks() {
        return Collections.unmodifiableCollection(sinks);
    }

    @Override
    public List<MixerFilter> addFilter(MixerFilter... filterChannels) {
        if (filterChannels.length != getAudioChannels())
            throw new IllegalArgumentException("invalid filter count: channel mismatch");

        List<MixerFilter> filterList = Collections.unmodifiableList(Arrays.asList(filterChannels));

        filters.add(filterList);

        return filterList;
    }

    @Override
    public boolean removeFilter(List<MixerFilter> filterList) {
        return filters.remove(filterList);
    }

    @Override
    public boolean addSink(MixerSink sink) {
        if (sink.getAudioFormat().getSampleRate() != getAudioSampleRate() ||
                sink.getAudioFormat().getChannels() != getAudioChannels())
            throw new IllegalArgumentException("sink format unacceptable");

        if (sinks.add(sink)) {
            if (isRunning()) sink.start();
            else sink.stop();

            return true;
        }

        return false;
    }

    @Override
    public boolean removeSink(MixerSink sink) {
        if (sinks.remove(sink)) {
            sink.stop();
            return true;
        } else return false;
    }

    @Override
    public Collection<MixerChannel> getChannels() {
        return Collections.unmodifiableCollection(channels);
    }

    @Override
    public boolean addChannel(MixerChannel channel) {
        synchronized (channelLock) {
            boolean added;

            if (channel.getSampleRate() != getAudioSampleRate() || channel.getChannels() != getAudioChannels())
                throw new IllegalArgumentException("format mismatch");

            synchronized (channelLock) {
                added = channels.add(channel);
            }

            if (!isPlaying()) setRunning(true);

            if (added) {
                // Events...
            }

            return added;
        }
    }

    @Override
    public boolean removeChannel(MixerChannel channel) {
        boolean removed, stopped;

        synchronized (channelLock) {
            boolean wasPlaying = isPlaying();
            removed = channels.remove(channel);
            stopped = removed && wasPlaying && !isPlaying();
        }

        if (stopped) setRunning(false);

        if (removed) {
            // Events...
        }

        return removed;
    }

    @Override
    public Collection<List<MixerFilter>> getFilters() {
        return Collections.unmodifiableCollection(filters);
    }

    @Override
    public boolean isRunning() {
        return getSinks().stream().anyMatch(MixerSink::isRunning);
    }

    @Override
    public boolean isPlaying() {
        return getChannels().size() > 0;
    }

    @Override
    public int available() {
        // Find out how much the sinks can flush down right now
        int sinkAvailable = Math.min(
                getBufferSize(),
                getSinks().stream().filter(MixerSink::isRunning).mapToInt(MixerSink::availableInput).min().orElse(0)
        );

        // Shortcut
        if (sinkAvailable <= 0) return 0;

        // Get the count of samples available in each channel, taking the minimum first
        int channelAvailable = getChannels()
                .stream()
                .filter(MixerChannel::isPlaying)
                .mapToInt(AudioProvider::available)
                .min()
                .orElse(0);

        // Shortcut
        if (channelAvailable <= 0) return 0;

        // Attempt to flush down the minimum amount of samples that all parties agree on
        return Math.min(sinkAvailable, channelAvailable);
    }

    @Override
    public void empty() {
        synchronized (channelLock) {

        }
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public float getAudioSampleRate() {
        return audioSampleRate;
    }

    @Override
    public int getAudioChannels() {
        return audioChannels;
    }

    @Override
    public boolean setRunning(boolean running) {
        if (running) {
            Logger.getGlobal().info("Starting mixer...");
            return getSinks().stream().filter(x -> !x.isRunning()).allMatch(MixerSink::start);
        } else {
            Logger.getGlobal().info("Stopping mixer...");
            boolean stopped = getSinks().stream().filter(MixerSink::isRunning).allMatch(MixerSink::stop);

            // If stopped, reset all filters.
            if (stopped) filters.forEach(filters -> filters.forEach(MixerFilter::reset));

            return stopped;
        }
    }
}
