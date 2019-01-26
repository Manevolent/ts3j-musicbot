package com.github.manevolent.ts3j.musicbot.audio.resample;

import com.github.manevolent.ffmpeg4j.FFmpeg;
import com.github.manevolent.ffmpeg4j.Logging;

import com.github.manevolent.ts3j.musicbot.audio.AudioBuffer;
import org.bytedeco.javacpp.*;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * NOTE: FFmpeg is very picky about what sample count you use. Must divide sample count by channel count.
 */

// For bugs: https://github.com/bytedeco/javacv/blob/master/src/main/java/org/bytedeco/javacv/FFmpegFrameRecorder.java
public class FFmpegResampler extends Resampler {
    private static final int SAMPLE_FORMAT = avutil.AV_SAMPLE_FMT_FLT;

    private volatile swresample.SwrContext swrContext;

    private final BytePointer[] samples_in;
    private final BytePointer[] samples_out;
    private final PointerPointer samples_in_ptr;
    private final PointerPointer samples_out_ptr;

    private ByteBuffer presampleOutputBuffer = ByteBuffer.allocate(0);
    private byte[] recvBuffer;
    private final ResamplerProperties input, output;
    private final SampleConverter sampleConverter;

    private final Object nativeLock = new Object();

    public FFmpegResampler(AudioFormat inputFormat, AudioFormat outputFormat, int bufferSize) {
        super(inputFormat, outputFormat);

        if (inputFormat.getChannels() <= 0)
            throw new IllegalArgumentException("invalid input channel count: " + inputFormat.getChannels());

        if (outputFormat.getChannels() <= 0)
            throw new IllegalArgumentException("invalid output channel count: " + outputFormat.getChannels());

        try {
            // Configure input parameters
            int ffmpegInputFormat;

            this.sampleConverter = SampleConverter.Depth.fromFormat(inputFormat);
            ffmpegInputFormat = SAMPLE_FORMAT;

            int inputChannels = inputFormat.getChannels();
            int inputPlanes = avutil.av_sample_fmt_is_planar(ffmpegInputFormat) != 0 ? inputChannels : 1;
            int inputSampleRate = (int)(inputFormat.getSampleRate());
            int inputBytesPerSample = avutil.av_get_bytes_per_sample(ffmpegInputFormat);
            int inputFrameSize = bufferSize * (inputFormat.getChannels() / inputPlanes) * 4; // x4 for float datatype

            this.input = new ResamplerProperties(
                    inputSampleRate,
                    inputBytesPerSample,
                    inputChannels,
                    inputPlanes,
                    inputFrameSize
            );

            // Configure output parameters
            int ffmpegOutputFormat = SAMPLE_FORMAT;
            int outputChannels = outputFormat.getChannels();
            int outputPlanes = avutil.av_sample_fmt_is_planar(ffmpegOutputFormat) != 0 ? outputChannels : 1;
            int outputSampleRate = (int)(outputFormat.getSampleRate());
            int outputBytesPerSample = avutil.av_get_bytes_per_sample(ffmpegOutputFormat);

            int outputFrameSize = avutil.av_samples_get_buffer_size(
                    (IntPointer) null,
                    outputChannels,
                    inputFrameSize, // Input frame size neccessary
                    ffmpegOutputFormat,
                    1
            ) / outputPlanes;

            this.output = new ResamplerProperties(
                    outputSampleRate,
                    outputBytesPerSample,
                    outputChannels,
                    outputPlanes,
                    outputFrameSize
            );

            swrContext = swresample.swr_alloc_set_opts(
                    null,

                    // Output configuration
                    FFmpeg.guessFFMpegChannelLayout(outputChannels),
                    ffmpegOutputFormat,
                    outputSampleRate,

                    // Input configuration
                    FFmpeg.guessFFMpegChannelLayout(inputChannels),
                    ffmpegInputFormat,
                    inputSampleRate,

                    0, null
            );

            // Force resampler to always resample regardless of the sample rates.
            // This forces the output to always be floats.
            avutil.av_opt_set_int(swrContext, "swr_flags", 1, 0);

            if (swresample.swr_init(swrContext) < 0) throw new RuntimeException("failed to initialize resampler");

            // Create input buffers
            samples_in = new BytePointer[inputPlanes];
            for (int i = 0; i < inputPlanes; i++) {
                samples_in[i] = new BytePointer(avutil.av_malloc(inputFrameSize)).capacity(inputFrameSize);
            }

            // Create output buffers
            samples_out = new BytePointer[outputPlanes];
            for (int i = 0; i < outputPlanes; i++) {
                samples_out[i] = new BytePointer(avutil.av_malloc(outputFrameSize)).capacity(outputFrameSize);
            }

            // Initialize input and output sample buffers;
            samples_in_ptr = new PointerPointer(inputPlanes);
            samples_out_ptr = new PointerPointer(outputPlanes);

            for (int i = 0; i < samples_out.length; i++)
                samples_out_ptr.put(i, samples_out[i]);

            for (int i = 0; i < samples_in.length; i++)
                samples_in_ptr.put(i, samples_in[i]);
        } catch (Throwable e) {
            if (swrContext != null) {
                swresample.swr_free(swrContext);
                swrContext = null;
            }

            throw new RuntimeException(e);
        }
    }

    public void close() throws Exception {
        synchronized (nativeLock) {
            // see: https://ffmpeg.org/doxygen/2.1/doc_2examples_2resampling_audio_8c-example.html
            for (int i = 0; i < samples_in.length; i++) {
                Logging.LOGGER.log(Logging.DEBUG_LOG_LEVEL, "deallocating samples_in[" + i + "])...");
                avutil.av_free(samples_in[i]);
                samples_in[i].deallocate();
                samples_in[i] = null;
            }
            Logging.LOGGER.log(Logging.DEBUG_LOG_LEVEL, "deallocating samples_in_ptr...");
            samples_in_ptr.deallocate();

            // see: https://ffmpeg.org/doxygen/2.1/doc_2examples_2resampling_audio_8c-example.html
            for (int i = 0; i < samples_out.length; i++) {
                Logging.LOGGER.log(Logging.DEBUG_LOG_LEVEL, "deallocating samples_out[" + i + "])...");
                avutil.av_free(samples_out[i]);
                samples_out[i].deallocate();
                samples_out[i] = null;
            }
            Logging.LOGGER.log(Logging.DEBUG_LOG_LEVEL, "deallocating samples_out_ptr...");
            samples_out_ptr.deallocate();

            // Prevent double-free (8/21/2017)
            if (swrContext != null && !swrContext.isNull())
                swresample.swr_free(swrContext);

            swrContext = null;
            presampleOutputBuffer = null;
            recvBuffer = null;
        }
    }

    @Override
    public int resample(byte[] frameData, int samples, AudioBuffer audioBuffer) {
        int ffmpegNativeLength = samples * 4;
        if (presampleOutputBuffer.capacity() < ffmpegNativeLength) {
            presampleOutputBuffer = ByteBuffer.allocate(ffmpegNativeLength);
            presampleOutputBuffer.order(ByteOrder.nativeOrder());
        }

        presampleOutputBuffer.clear();

        sampleConverter.convert(
                frameData,
                getInputFormat().isBigEndian(),
                presampleOutputBuffer.asFloatBuffer(),
                samples
        );

        return resample(presampleOutputBuffer, samples, audioBuffer);
    }

    @Override
    public int resample(ByteBuffer floatBufferAsBytes, int samples, AudioBuffer out) {
        synchronized (nativeLock) {
            if (swrContext == null || swrContext.isNull())
                throw new RuntimeException("swrContext is null; context has been closed.");

            int outputCount =
                    (int) Math.min(
                            (samples_out[0].limit() - samples_out[0].position()) / (output.getChannels() * output.getBytesPerSample()),
                            out.availableInput() // 10/14/18: overflow check
                    );

            samples_in[0].position(0).put(floatBufferAsBytes.array(), 0, samples * 4);

            //Returns number of samples output per channel, negative value on error
            int ret = swresample.swr_convert(
                    swrContext,
                    samples_out_ptr, outputCount,
                    samples_in_ptr, samples / input.getChannels()
            );

            // Check return values
            if (ret < 0) throw new RuntimeException("swr_convert failed: returned " + ret);
            else if (ret == 0) return 0; // Do nothing.

            // Read native sample buffer(s) into managed raw byte array
            // WARNING: This only works if the output format is non-planar (doesn't end with "P")

            int returnedSamples = ret * output.getChannels();
            int len = returnedSamples * output.getBytesPerSample();
            if (recvBuffer == null || recvBuffer.length < len)
                recvBuffer = new byte[len];

            samples_out[0].position(0).get(recvBuffer);

            // Convert raw data to bytes.
            // This is done by converting the raw samples to floats right out of ffmpeg to preserve the
            // original quality post-resample.

            FloatBuffer buffer = ByteBuffer.wrap(recvBuffer).order(ByteOrder.nativeOrder()).asFloatBuffer();
            buffer.position(0).limit(returnedSamples);

            // Return total re-sampled bytes to the higher-level audio system.
            int stored = out.write(buffer, returnedSamples);

            if (stored != returnedSamples)
                throw new IllegalStateException(
                        "failed to store samples in resample buffer: "
                                + returnedSamples + " != " + stored
                );

            return stored;
        }
    }

    @Override
    public int flush(AudioBuffer out) {
        return resample(ByteBuffer.allocate(0), 0, out);
    }

    public static class FFmpegResamplerFactory implements ResamplerFactory {
        @Override
        public Resampler create(AudioFormat in, AudioFormat out, int bufferSize) {
            return new FFmpegResampler(in, out, bufferSize);
        }
    }

    private static class ResamplerProperties {
        private final int sampleRate, bytesPerSample, channels, planes, frame_size;

        private ResamplerProperties(int sampleRate,int bytesPerSample, int channels, int planes, int frame_size) {
            this.sampleRate = sampleRate;
            this.bytesPerSample = bytesPerSample;
            this.channels = channels;
            this.planes = planes;
            this.frame_size = frame_size;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public int getBytesPerSample() {
            return bytesPerSample;
        }

        public int getChannels() {
            return channels;
        }

        public int getPlanes() {
            return planes;
        }

        public int getFrameSize() {
            return frame_size;
        }
    }
}
