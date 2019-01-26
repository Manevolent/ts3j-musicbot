package com.github.manevolent.ts3j.musicbot.audio.resample;

public final class SampleConvert {
    private static final int _24MIN = (int) Math.pow(-2, 23);
    private static final int _24MAX = (int) Math.pow(2, 23) - 1;

    public static final float byteToFloat(byte b) {
        return ((float)(b - Byte.MIN_VALUE) / (float)(Byte.MAX_VALUE - Byte.MIN_VALUE))*2f - 1f;
    }

    public static final float shortToFloat(short s) {
        return ((float)(s - Short.MIN_VALUE) / (float)(Short.MAX_VALUE - Short.MIN_VALUE))*2f - 1f;
    }

    public static final float int24ToFloat(int s) {
        return ((float)(s - _24MIN) / (float)(_24MAX - _24MIN))*2f - 1f;
    }

    public static final float int32ToFloat(int s) {
        return ((float)(s - Integer.MIN_VALUE) / (float)(Integer.MAX_VALUE - Integer.MIN_VALUE))*2f - 1f;
    }

    public static final short floatToShort(float f) {
        return (short)
                Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE,
                                Math.floor(Short.MIN_VALUE + ((Short.MAX_VALUE - Short.MIN_VALUE) * ((f+1f)/2f))))
                );
    }


    public static final int floatToInt24(float f) {
        return (int)
                Math.max(_24MIN,
                        Math.min(_24MAX,
                                Math.floor(_24MIN + ((_24MAX - _24MIN) * ((f+1f)/2f))))
                );
    }

    public static final int floatToInt32(float f) {
        return (int)
                Math.max(Integer.MIN_VALUE,
                        Math.min(Integer.MAX_VALUE,
                                Math.floor(Integer.MIN_VALUE + (long)(((long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE) * ((f+1f)/2f))))
                );
    }

    public static final byte[] shortToPCM(byte[] buf, short[] s, int offs, int len) {
        for (int i = offs; i < len+offs; i ++) {
            buf[(i*2)+1] = (byte)(s[i] & 0xff);
            buf[(i*2)] = (byte)((s[i] >> 8) & 0xff);
        }
        return buf;
    }

    public static final short[] pcmToShort(byte[] buf, short[] s, int offs, int len) {
        for (int i = offs; i < len+offs; i ++)
            s[i] = (short)( ((buf[(i*2)+1]&0xFF)<<8) | (buf[(i*2)]&0xFF) );
        return s;
    }

    public static void swapEndianness(byte[] buf) {
        byte b;
        for (int i = 0; i < buf.length; i += 2) {
            b = buf[i];
            buf[i] = buf[i+1];
            buf[i+1] = b;
        }
    }

    public static int bytesToInt32_optimized_LE( byte [] buffer, int byteOffset)
    {
        return ((buffer[byteOffset+3] << 24) + (buffer[byteOffset+2] << 16) + (buffer[byteOffset+1] << 8) + (buffer[byteOffset] << 0));
    }

    public static int bytesToInt32_optimized_BE( byte [] buffer, int byteOffset)
    {
        return ((buffer[byteOffset] << 24) + (buffer[byteOffset+1] << 16) + (buffer[byteOffset+2] << 8) + (buffer[byteOffset+3] << 0));
    }

    public static int bytesToInt24_optimized_LE( byte [] buffer, int byteOffset)
    {
        return ((buffer[byteOffset+2] << 16) + (buffer[byteOffset+1] << 8) + (buffer[byteOffset] << 0));
    }

    public static int bytesToInt24_optimized_BE( byte [] buffer, int byteOffset)
    {
        return ((buffer[byteOffset] << 16) + (buffer[byteOffset+1] << 8) + (buffer[byteOffset+2] << 0));
    }

    public static int bytesToInt16_optimized_LE( byte [] buffer, int byteOffset)
    {
        return ((buffer[byteOffset+1]<<8) | (buffer[byteOffset] & 0xFF));
    }

    public static int bytesToInt16_optimized_BE( byte [] buffer, int byteOffset)
    {
        return ((buffer[byteOffset]<<8) | (buffer[byteOffset+1] & 0xFF));
    }

    public static void intToBytes16_optimized_LE( int sample, byte [] buffer, int byteOffset)
    {
        buffer[byteOffset]=( byte ) (sample & 0xFF);
        buffer[byteOffset + 1]=( byte ) ((sample >> 8) & 0xFF);
    }

    public static void intToBytes24_optimized_LE( int sample, byte [] buffer, int byteOffset)
    {
        buffer[byteOffset]=( byte ) (sample & 0xFF);
        buffer[byteOffset + 1]=( byte ) ((sample >> 8) & 0xFF);
        buffer[byteOffset + 2]=( byte ) ((sample >> 16) & 0xFF);
    }

    public static void intToBytes32_optimized_LE( int sample, byte [] buffer, int byteOffset)
    {
        buffer[byteOffset]=( byte ) (sample & 0xFF);
        buffer[byteOffset + 1]=( byte ) ((sample >> 8) & 0xFF);
        buffer[byteOffset + 2]=( byte ) ((sample >> 16) & 0xFF);
        buffer[byteOffset + 3]=( byte ) ((sample >> 24) & 0xFF);
    }

}
