package com.github.manevolent.ts3j.musicbot.audio.mixer.input;

import java.io.IOException;

public interface AudioProvider extends AutoCloseable {

    /**
     * Gets count of available samples
     * @return Samples available
     */
    int available();

    /**
     * Read samples.  This is a non-blocking operation.
     * @param buffer Sample buffer.
     * @param len Sample length to read.
     * @return Samples filled into the target buffer.
     * @throws IOException
     */
    int read(float[] buffer, int offs, int len) throws IOException;

}