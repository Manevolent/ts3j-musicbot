package com.github.manevolent.ts3j.musicbot.audio.mixer.output;

import javax.sound.sampled.AudioFormat;

public interface MixerSink {

    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Gets the audio format of the sink.
     * @return Audio format.
     */
    AudioFormat getAudioFormat();

    /**
     * Writes a set of float samples, interleaved by channel, to the sink.
     * @param buffer Sample buffer
     * @param len Length of sample buffer to copy into the sink.
     */
    void write(float[] buffer, int len);

    /**
     * Gets the total count of samples available for writing to.  Used by the mixer system to ensure not more than
     * the available sample count is flushed down the sink (CPU resource saver)
     * @return
     */
    int availableInput();

    /**
     * Gets if the sink is running.  Do not write to the sink if isRunning() returns false.
     * @return true if the sink was started with start() and stop() hasn't been called, false otherwise.
     */
    boolean isRunning();

    /**
     * Starts the sink, enabling it to receive data.  Will make isRunning() return true.
     */
    boolean start();

    /**
     * Stops the sink, disabling it from receiving any data.
     */
    boolean stop();

    /**
     * Gets the total buffer size of the sink in samples.
     * @return Buffer size.
     */
    int getBufferSize();

    /**
     * Gets position of the mixer sink.  This may not be the network position.
     *
     * @return Position in samples
     */
    long getPosition();

    /**
     * Gets count of stream underflow operations, indicating the number of times that the sink has
     * detected that not enough data is flowing into the sink.
     * @return Count of underflow operations
     */
    long getUnderflows();

    /**
     * Gets the count of stream overflow operations, indicating  the number of times that the sink has
     * detected that too much data is flowing into the sink.  Typical situation is where write() will return 0.
     * @return Count of overflow operations.
     */
    long getOverflows();

    /**
     * Closes the sink permanently.
     */
    default void close() {
        if (isRunning()) stop();
    }

}
