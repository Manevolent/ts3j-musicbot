package com.github.manevolent.ts3j.musicbot.download;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;

public class DownloadManager {
    private final LinkedList<DownloadSource> sources = new LinkedList<>();

    public DownloadManager() {

    }

    public void registerSource(DownloadSource source) {
        sources.add(source);
    }

    public void unregisterSource(DownloadSource source) {
        sources.remove(source);
    }

    public Download get(URI uri) throws IOException {
        for (DownloadSource source : sources) {
            if (source.canDownload(uri)) return source.get(uri);
        }

        throw new IllegalArgumentException("no sources applicable for URI");
    }
}
