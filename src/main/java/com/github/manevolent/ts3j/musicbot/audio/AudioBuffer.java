package com.github.manevolent.ts3j.musicbot.audio;

import java.nio.FloatBuffer;

public class AudioBuffer {
    private final Object resampleLock = new Object();
    private final Object mixLock = new Object();

    private final int size;
    private final float[] buffer;

    private volatile int len;

    public AudioBuffer(int size) {
        this.buffer = new float[this.size = size];
    }

    public int availableInput() {
        return getBufferSize() - availableOutput();
    }

    public int availableOutput() {
        return len;
    }

    public int getBufferSize() {
        return size;
    }

    public int write(float f) {
        if (this.size - this.len >= 1) {
            buffer[len] = f;
            this.len++;

            if (this.len > 0)
                synchronized (mixLock) {
                    mixLock.notifyAll();
                }

            return 1;
        } return 0;
    }


    public int write(FloatBuffer floatBuffer, int len) {
        if (len > floatBuffer.limit()) throw new ArrayIndexOutOfBoundsException(len);

        floatBuffer.get(buffer, this.len, len);
        this.len += len;

        if (this.len > 0)
            synchronized (mixLock) {
                mixLock.notifyAll();
            }

        return len;
    }

    public int write(float[] in, int len) {
        return write(in, 0, len);
    }

    public int write(float[] in, int offs, int len) {
        if (len > in.length-offs) throw new ArrayIndexOutOfBoundsException(len);

        int i;
        for (i = 0; i < len && this.len+i < buffer.length; i ++)
            buffer[this.len+i] = in[i+offs];

        this.len += i;

        if (this.len > 0)
            synchronized (mixLock) {
                mixLock.notifyAll();
            }

        return i;
    }

    public int mix(float[] out, int offs, int len) {
        synchronized (resampleLock) {
            int x = Math.min(len, this.len);
            if (x <= 0) return 0;

            for (int i = 0; i < x; i ++) out[i+offs] = out[i+offs] + buffer[i];

            // Resize buffer
            this.len -= x;

            // Reclaim free bytes
            if (this.len > 0) {
                System.arraycopy(buffer, x, buffer, 0, this.len);
            } else {
                this.len = 0;
            }

            resampleLock.notify();

            return x;
        }
    }
}
