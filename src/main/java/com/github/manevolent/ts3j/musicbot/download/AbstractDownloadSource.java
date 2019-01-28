package com.github.manevolent.ts3j.musicbot.download;

public abstract class AbstractDownloadSource implements DownloadSource {
    private final String name;

    protected AbstractDownloadSource(String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }
}
