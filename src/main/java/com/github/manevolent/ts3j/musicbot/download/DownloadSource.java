package com.github.manevolent.ts3j.musicbot.download;

import java.io.IOException;
import java.net.URI;

public interface DownloadSource {
    String getName();
    boolean canDownload(URI uri);
    Download get(URI uri) throws IOException;
}
