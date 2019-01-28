package com.github.manevolent.ts3j.musicbot.download;

import com.github.manevolent.ts3j.musicbot.audio.player.AudioPlayer;

import java.io.IOException;
import java.util.Map;

public interface Download {
    Map<String, String> getMetadata();

    DownloadSource getSource();

    default String getTitle() {
        return getMetadata().get("title");
    }

    AudioPlayer openPlayer() throws IOException;
}
