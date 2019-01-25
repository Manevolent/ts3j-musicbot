package com.github.manevolent.ts3j.musicbot.command;

public interface CommandSender {

    String getName();

    void sendMessage(String message);

}
