package com.github.manevolent.ts3j.musicbot.download;

import java.util.Map;

public abstract class AbstractDownload implements Download {
    private final Map<String, String> metadata;
    private final DownloadSource source;

    public AbstractDownload(Map<String, String> metadata, DownloadSource source) {
        this.metadata = metadata;
        this.source = source;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public DownloadSource getSource() {
        return source;
    }
}
