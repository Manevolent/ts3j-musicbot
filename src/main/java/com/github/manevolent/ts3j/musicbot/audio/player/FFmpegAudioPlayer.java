package com.github.manevolent.ts3j.musicbot.audio.player;

import com.github.manevolent.ffmpeg4j.AudioFrame;
import com.github.manevolent.ffmpeg4j.FFmpegException;
import com.github.manevolent.ffmpeg4j.FFmpegInput;
import com.github.manevolent.ffmpeg4j.MediaType;
import com.github.manevolent.ffmpeg4j.source.AudioSourceSubstream;
import com.github.manevolent.ffmpeg4j.source.MediaSourceSubstream;
import com.github.manevolent.ffmpeg4j.stream.source.FFmpegSourceStream;

import org.bytedeco.javacpp.avformat;

import javax.sound.sampled.AudioFormat;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class FFmpegAudioPlayer extends BufferedAudioPlayer {
    private final AudioSourceSubstream substream;
    private final Object nativeLock = new Object();
    private volatile boolean eof, closed = false;
    private final AudioFormat format;

    public FFmpegAudioPlayer(AudioSourceSubstream substream, int bufferSize) {
        super(bufferSize);

        this.substream = substream;

        this.format = new AudioFormat(
                substream.getFormat().getSampleRate(),
                32,
                substream.getFormat().getChannels(),
                true,
                false
        );
    }

    public boolean hasReachedEof() {
        return eof;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isPlaying() {
        return !isClosed() && (!hasReachedEof() || getBuffer().availableOutput() > 0);
    }

    @Override
    public boolean stop() {
        return kill();
    }

    public boolean processBuffer() throws IOException {
        synchronized (nativeLock) {
            if (eof) return false;

            AudioFrame frame;

            try {
                frame = substream.next();
            } catch (EOFException ex) {
                // EOF
                eof = true;
                return false;
            }

            if (frame == null)
                return true; // try again

            if (frame.getSamples().length <= 0)
                return true; // try again

            getBuffer().write(frame.getSamples(), frame.getLength());

            return true;
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (nativeLock) {
            if (!closed) {
                substream.getParent().close();
                eof = true;
                closed = true;
            }
        }
    }

    public final AudioFormat getFormat() {
        return format;
    }

    /**
     * Opens a new player with an FFmpeg input source.
     *
     * @param inputFormat Input format
     * @param inputStream Input stream
     * @return
     * @throws FFmpegException
     */
    public static FFmpegAudioPlayer open(
            avformat.AVInputFormat inputFormat,
            InputStream inputStream,
            int mixerBufferSize
    ) throws FFmpegException {
        FFmpegInput input = new FFmpegInput(inputStream);
        FFmpegSourceStream stream = input.open(inputFormat);
        for (MediaSourceSubstream substream : stream.registerStreams()) {
            if (substream.getMediaType() != MediaType.AUDIO) continue;

            AudioSourceSubstream audioSourceSubstream = (AudioSourceSubstream) substream;

            return new FFmpegAudioPlayer(
                    audioSourceSubstream,
                    mixerBufferSize
            );
        }

        throw new FFmpegException("no audio substreams found in input: " + inputFormat.name().getString());
    }
}
