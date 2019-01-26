package com.github.manevolent.ts3j.musicbot.audio.resample;

import javax.sound.sampled.AudioFormat;
import java.nio.FloatBuffer;

public interface SampleConverter {
    void convert(byte[] pcm, boolean bigEndian, FloatBuffer target, int samples);

    enum Depth implements SampleConverter {
        SIGNED_32BIT(32, true) {
            @Override
            public void convert(byte[] pcm, boolean bigEndian, FloatBuffer target, int samples) {
                if (bigEndian)
                    for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                        target.put(SampleConvert.int32ToFloat((short) SampleConvert.bytesToInt32_optimized_BE(pcm, sampleIndex * 4)));
                else
                    for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                        target.put(SampleConvert.int32ToFloat((short) SampleConvert.bytesToInt32_optimized_LE(pcm, sampleIndex * 4)));
            }
        },
        SIGNED_24BIT(24, true) {
            @Override
            public void convert(byte[] pcm, boolean bigEndian, FloatBuffer target, int samples) {
                if (bigEndian)
                    for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                        target.put(SampleConvert.int24ToFloat((short) SampleConvert.bytesToInt24_optimized_BE(pcm, sampleIndex * 3)));
                else
                    for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                        target.put(SampleConvert.int24ToFloat((short) SampleConvert.bytesToInt24_optimized_LE(pcm, sampleIndex * 3)));
            }
        },
        SIGNED_16BIT(16, true) {
            @Override
            public void convert(byte[] pcm, boolean bigEndian, FloatBuffer target, int samples) {
                if (bigEndian)
                    for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                        target.put(SampleConvert.shortToFloat((short) SampleConvert.bytesToInt16_optimized_BE(pcm, sampleIndex * 2)));
                else
                    for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                        target.put(SampleConvert.shortToFloat((short) SampleConvert.bytesToInt16_optimized_LE(pcm, sampleIndex * 2)));
            }
        },
        SIGNED_8BIT(8, true) {
            @Override
            public void convert(byte[] pcm, boolean bigEndian, FloatBuffer target, int samples) {
                for (int sampleIndex = 0; sampleIndex < samples; sampleIndex ++)
                    target.put(SampleConvert.byteToFloat(pcm[sampleIndex]));
            }
        },
        DISABLED_0BIT(0, true) {
            @Override
            public void convert(byte[] pcm, boolean bigEndian, FloatBuffer target, int samples) {
                throw new UnsupportedOperationException();
            }
        };

        private final int bitDepth;
        private final boolean signed;

        Depth(int bitDepth, boolean signed) {
            this.bitDepth = bitDepth;
            this.signed = signed;
        }

        public static Depth fromFormat(AudioFormat audioFormat) {
            boolean signed;

            switch (audioFormat.getEncoding().toString()) {
                case "PCM_SIGNED":
                    signed = true;
                    break;
                case "PCM_UNSIGNED":
                    signed = false;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported encoding: " + audioFormat.getEncoding().toString());
            }

            for (Depth depth : values()) {
                if (depth.bitDepth == audioFormat.getSampleSizeInBits() && depth.signed == signed)
                    return depth;
            }

            throw new IllegalArgumentException(
                    "Unsupported PCM format: " +
                    audioFormat.getSampleSizeInBits() + "bit (signed=" + signed + ")"
            );
        }

        public int getBitDepth() {
            return bitDepth;
        }

        public boolean isSigned() {
            return signed;
        }
    }
}
