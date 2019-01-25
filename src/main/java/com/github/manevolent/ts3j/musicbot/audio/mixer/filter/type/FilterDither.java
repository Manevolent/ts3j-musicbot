package com.github.manevolent.ts3j.musicbot.audio.mixer.filter.type;

import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.MixerFilter;

import java.util.Random;

/**
 * Applies a digital filter to the output signal to reduce PCM rounding errors.
 * By understanding the output bit depth (16 bit, normally), we can apply a certain amount
 * of randomness to the output to encourage the bits to move enough to represent the true value
 * of the waveforms.
 *
 * from Wikipedia:
 * This leads to the dither solution. Rather than predictably rounding up or down in a repeating pattern,
 * it is possible to round up or down in a random pattern. Dithering is a way to randomly toggle the results
 * between 4 and 5 so that 80% of the time it ended up on 5 then it would average 4.8 over the long run but
 * would have random, non-repeating error in the result.
 */
public class FilterDither implements MixerFilter {
    private final Random random;
    private final float ditherRange;

    public FilterDither(int bits) {
        random = new Random(bits ^ 0xDEADBEEF);
        ditherRange = (1f / (float)Math.pow(2, bits));
    }

    @Override
    public int process(float[] samples, int offs, int len) {
        for (int i = offs; i < len; i ++) {
            if (samples[i] % 1f != 0f) // Only dither if sample has a remainder
                samples[i] += ditherRange * (random.nextFloat());
        }

        return len - offs;
    }
}
