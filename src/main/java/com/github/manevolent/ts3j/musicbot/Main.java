package com.github.manevolent.ts3j.musicbot;

import com.github.manevolent.ffmpeg4j.FFmpeg;
import com.github.manevolent.ffmpeg4j.FFmpegException;
import com.github.manevolent.ts3j.musicbot.download.DownloadManager;
import com.github.manevolent.ts3j.musicbot.download.YoutubeDLSource;

import java.io.File;
import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws IOException, FFmpegException {
        FFmpeg.register();

        DownloadManager downloadManager = new DownloadManager();

        // Get Youtube-dl as an executable: https://rg3.github.io/youtube-dl/
        // I do NOT include an EXE in this repository.
        downloadManager.registerSource(new YoutubeDLSource(new File("youtube-dl.exe")));


    }
}
