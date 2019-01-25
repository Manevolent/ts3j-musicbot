package com.github.manevolent.ts3j.musicbot.audio.mixer.filter.type;

import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.MixerFilter;

public class SoftClipFilter implements MixerFilter {
    private final int channels;
    private final float[] states;

    public SoftClipFilter(int channels) {
        this.channels = channels;
        this.states = new float[channels];
    }

    public SoftClipFilter() {
        this(1);
    }

    @Override
    public void reset() {
        for (int i = 0; i < channels; i ++)
            states[i] = 0f;
    }

    @Override
    public int process(float[] samples, int offs, int len) {
        pcm_soft_clip(samples, offs, len/channels, channels, states);

        return len - offs;
    }

    /**
     * Stolen from OPUS codec:
     *  opus_pcm_soft_clip
     *
     * https://opus-codec.org/docs/opus_api-1.1.2/group__opus__decoder.html#gaff99598b352e8939dded08d96e125e0b
     * @param _x Input PCM and modified PCM
     * @param N Number of samples per channel to process
     * @param C Number of channels
     * @param declip_mem State memory for the soft clipping process (one float per channel, initialized to zero)
     */
    private static void pcm_soft_clip(float[] _x, int offs, int N, int C, float[] declip_mem) {
        int c;
        int i;

        if (C<1 || N<1) return;

        /* First thing: saturate everything to +/- 2 which is the highest level our
          non-linearity can handle. At the point where the signal reaches +/-2,
          the derivative will be zero anyway, so this doesn't introduce any
          discontinuity in the derivative. */
        for (i=0;i<N*C;i++)
            _x[offs + i] = Math.max(-2.f, Math.min(2.f, _x[offs + i]));

        for (c=0;c<C;c++)
        {
            float a;
            float x0;
            int curr;

            a = declip_mem[c];

            /* Continue applying the non-linearity from the previous frame to avoid
                any discontinuity. */
            for (i=0;i<N;i++)
            {
                if (_x[offs + c + (i*C)]*a>=0)  // (pointer) if (x[i*C]*a>=0)
                    break;

                _x[offs + c + (i*C)] = _x[offs + c + (i*C)]+a*_x[offs + c + (i*C)]*_x[offs + c + (i*C)]; // (pointer) x[i*C] = x[i*C]+a*x[i*C]*x[i*C];
            }

            curr=0;
            x0 = _x[offs + c]; // (pointer) x0 = x[0];
            while(true)
            {
                int start, end;
                float maxval;
                int special=0;
                int peak_pos;
                for (i=curr;i<N;i++)
                {
                    if (_x[offs + c + (i*C)]>1 || _x[offs + c + (i*C)]<-1) // if (x[i*C]>1 || x[i*C]<-1)
                        break;
                }
                if (i==N)
                {
                    a=0;
                    break;
                }
                peak_pos = i;
                start=end=i;
                maxval=Math.abs(_x[offs + c + (i*C)]); //  maxval=ABS16(x[i*C]);
                /* Look for first zero crossing before clipping */
                while (start>0 && _x[offs + c + (i*C)]*_x[offs + c + ((start-1)*C)]>=0) // while (start>0 && x[i*C]*x[(start-1)*C]>=0)
                    start--;
                /* Look for first zero crossing after clipping */
                while (end<N && _x[offs + c + (i*C)]*_x[offs + c + (end*C)]>=0) //  while (end<N && x[i*C]*x[end*C]>=0)
                {
                    /* Look for other peaks until the next zero-crossing. */
                    if (Math.abs(_x[offs + c + (end*C)])>maxval) // if (ABS16(x[end*C])>maxval)
                    {
                        maxval = Math.abs(_x[offs + c + (end*C)]); //  maxval = ABS16(x[end*C]);
                        peak_pos = end;
                    }
                    end++;
                }
                /* Detect the special case where we clip before the first zero crossing */
                special = (start==0 && _x[offs + c + (i*C)]*_x[offs + c]>=0) ? 1 : 0;

                /* Compute a such that maxval + a*maxval^2 = 1 */
                a=(maxval-1)/(maxval*maxval);
                /* Slightly boost "a" by 2^-22. This is just enough to ensure -ffast-math
                does not cause output values larger than +/-1, but small enough not
                to matter even for 24-bit output.  */
                a += a*2.4e-7f;
                if (_x[offs + c + (i*C)]>0)
                    a = -a;
                /* Apply soft clipping */
                for (i=start;i<end;i++)
                    _x[offs + c + (i*C)] = _x[offs + c + (i*C)]+a*_x[offs + c + (i*C)]*_x[offs + c + (i*C)];

                if (special == 1 && peak_pos>=2)
                {
                    /* Add a linear ramp from the first sample to the signal peak.
                       This avoids a discontinuity at the beginning of the frame. */
                    float delta;
                    float offset = x0-_x[offs + c];
                    delta = offset / peak_pos;
                    for (i=curr;i<peak_pos;i++)
                    {
                        offset -= delta;
                        _x[offs + c + (i*C)] += offset;
                        _x[offs + c + (i*C)] = Math.max(-1.f, Math.min(1.f, _x[offs + c + (i*C)]));
                    }
                }
                curr = end;
                if (curr==N)
                    break;
            }
            declip_mem[c] = a;
        }
    }
}
