package com.github.manevolent.ts3j.musicbot.audio.player;

import com.github.manevolent.ts3j.musicbot.audio.AudioBuffer;

import java.io.IOException;

public abstract class BufferedAudioPlayer extends AudioPlayer {
    private final int bufferSize;
    private final AudioBuffer buffer;

    public BufferedAudioPlayer(int bufferSize) {
        this.bufferSize = bufferSize;
        this.buffer = new AudioBuffer(bufferSize);
    }

    @Override
    public int available() {
        return isPlaying() ? buffer.getBufferSize() : buffer.availableOutput();
    }

    /**
     * Advances playback when the buffer is too small.
     * @return true if playback should continue, false otherwise.
     */
    public abstract boolean processBuffer() throws IOException;

    protected final AudioBuffer getBuffer() {
        return buffer;
    }

    public final int getBufferSize() {
        return bufferSize;
    }

    @Override
    public int read(float[] buffer, int len) throws IOException {
        if (len > bufferSize)
            throw new ArrayIndexOutOfBoundsException(
                    "cannot read from stream: sample length requested too large ("
                            + len + " > " + bufferSize + ")."
            );

        int offs = 0;
        boolean eof = false;
        while (!eof && offs < len) {
            while (!eof && this.buffer.availableOutput() <= 0) {
                eof = !processBuffer();
            }

            offs += this.buffer.mix(
                    buffer,
                    offs,
                    Math.min(
                            len - offs,
                            this.buffer.availableOutput()
                    )
            );
        }

        return offs;
    }
}
