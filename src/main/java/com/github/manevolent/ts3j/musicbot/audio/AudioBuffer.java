package com.github.manevolent.ts3j.musicbot.audio;

import com.github.manevolent.ts3j.musicbot.audio.mixer.input.AudioProvider;

import java.io.IOException;
import java.nio.FloatBuffer;

public class AudioBuffer {
    private final Object resampleLock = new Object();
    private final Object mixLock = new Object();

    private final int size;
    private final float[] buffer;

    private volatile int positon;

    public AudioBuffer(int size) {
        this.buffer = new float[this.size = size];
    }

    public int availableInput() {
        return getBufferSize() - availableOutput();
    }

    public int availableOutput() {
        return positon;
    }

    public int getBufferSize() {
        return size;
    }

    public int write(AudioProvider provider, int len) throws IOException {
        if (len > provider.available()) throw new ArrayIndexOutOfBoundsException("provider");
        if (len > availableInput()) throw new ArrayIndexOutOfBoundsException();

        int read = provider.read(buffer, positon, len);
        this.positon += read;

        return read;
    }

    public int write(float f) {
        if (this.size - this.positon >= 1) {
            buffer[positon] = f;
            this.positon++;

            return 1;
        } return 0;
    }


    public int write(FloatBuffer floatBuffer, int len) {
        if (len > floatBuffer.limit()) throw new ArrayIndexOutOfBoundsException(len);

        floatBuffer.get(buffer, this.positon, len);
        this.positon += len;

        return len;
    }

    public int write(float[] in, int len) {
        return write(in, 0, len);
    }

    public int write(float[] in, int offs, int len) {
        if (len > in.length-offs) throw new ArrayIndexOutOfBoundsException(len);

        int i;
        for (i = 0; i < len && this.positon +i < buffer.length; i ++)
            buffer[this.positon +i] = in[i+offs];

        this.positon += i;

        return i;
    }

    public int mix(float[] out, int offs, int len) {
        int x = Math.min(len, this.positon);
        if (x <= 0) return 0;

        for (int i = 0; i < x; i ++) out[i+offs] = out[i+offs] + buffer[i];

        // Resize buffer
        this.positon -= x;

        // Reclaim free bytes
        if (this.positon > 0) {
            System.arraycopy(buffer, x, buffer, 0, this.positon);
        } else {
            this.positon = 0;
        }

        return x;
    }

    public int read(float[] out, int offs, int len) {
        int x = Math.min(len, this.positon);
        if (x <= 0) return 0;

        for (int i = 0; i < x; i ++) out[i+offs] = buffer[i];

        // Resize buffer
        this.positon -= x;

        // Reclaim free bytes
        if (this.positon > 0) {
            System.arraycopy(buffer, x, buffer, 0, this.positon);
        } else {
            this.positon = 0;
        }

        return x;
    }
}
