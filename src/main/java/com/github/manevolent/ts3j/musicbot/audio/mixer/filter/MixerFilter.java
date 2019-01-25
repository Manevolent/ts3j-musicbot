package com.github.manevolent.ts3j.musicbot.audio.mixer.filter;

/**
 * Represents a mono filter.  Stereo separation occurs in the mixer.
 */
public interface MixerFilter {

    /**
     * Calls the filter to process the specified audio buffer.
     *
     * @param buffer Mono PCM samples to process
     * @param offs offset to process at
     * @param len length of samples to process
     * @return processed samples, usually must == len
     */
    int process(float[] buffer, int offs, int len);

    /**
     * Resets the filter's state back to the initial filter state.
     */
    default void reset() { }

}
