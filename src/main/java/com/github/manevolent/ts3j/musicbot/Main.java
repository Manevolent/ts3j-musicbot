package com.github.manevolent.ts3j.musicbot;

import com.github.manevolent.ffmpeg4j.FFmpeg;
import com.github.manevolent.ts3j.musicbot.client.TeamspeakBot;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;

public final class Main {
    public static void main(String[] args) throws Exception {
        FFmpeg.register();

        File configurationFile = new File("config.json");
        if (!configurationFile.exists())
            IOUtils.copy(Main.class.getResourceAsStream("/config.json"), new FileOutputStream(configurationFile));

        JsonObject jsonObject = new JsonParser().parse(new FileReader(configurationFile)).getAsJsonObject();
        JsonObject defaultConfiguration = jsonObject.get("default").getAsJsonObject();
        JsonArray servers = jsonObject.get("servers").getAsJsonArray();

        List<TeamspeakBot> bots = new LinkedList<>();

        for (JsonElement serverElement : servers) {
            JsonObject serverObject = serverElement.getAsJsonObject();
            bots.add(new TeamspeakBot(defaultConfiguration, serverObject));
        }

        for (TeamspeakBot bot : bots) bot.setRunning(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (TeamspeakBot bot : bots) bot.setRunning(false);
        }));

        Object lock = new Object();
        synchronized (lock) {
            lock.wait();
        }
    }
}
