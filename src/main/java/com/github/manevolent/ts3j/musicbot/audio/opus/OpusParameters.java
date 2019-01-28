package com.github.manevolent.ts3j.musicbot.audio.opus;

import com.github.manevolent.ts3j.musicbot.config.ConfigurationHelper;
import com.google.gson.JsonObject;

public class OpusParameters {
    private final int opusFrameRate;
    private final int opusBitrate;
    private final int opusComplexity;
    private final int opusPacketLossPercent;
    private final boolean opusVbr;
    private final boolean opusFec;
    private final boolean opusMusic;

    public OpusParameters(int opusFrameRate, int opusBitrate,
                          int opusComplexity, int opusPacketLossPercent,
                          boolean opusVbr, boolean opusFec, boolean opusMusic) {
        this.opusFrameRate = opusFrameRate;
        this.opusBitrate = opusBitrate;
        this.opusComplexity = opusComplexity;
        this.opusPacketLossPercent = opusPacketLossPercent;
        this.opusVbr = opusVbr;
        this.opusFec = opusFec;
        this.opusMusic = opusMusic;
    }

    public int getOpusFrameTime() {
        return 1000 / opusFrameRate;
    }

    public int getOpusBitrate() {
        return opusBitrate;
    }

    public int getOpusComplexity() {
        return opusComplexity;
    }

    public int getOpusPacketLossPercent() {
        return opusPacketLossPercent;
    }

    public boolean isOpusVbr() {
        return opusVbr;
    }

    public boolean isOpusFec() {
        return opusFec;
    }

    public boolean isOpusMusic() {
        return opusMusic;
    }

    public static OpusParameters fromJson(JsonObject defaultConfiguration, JsonObject serverConfiguration) {
        return new OpusParameters(
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.framerate").getAsInt(),
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.bitrate").getAsInt(),
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.complexity").getAsInt(),
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.plc").getAsInt(),
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.vbr").getAsBoolean(),
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.fec").getAsBoolean(),
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.music").getAsBoolean()
        );
    }
}