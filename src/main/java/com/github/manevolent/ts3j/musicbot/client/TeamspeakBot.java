package com.github.manevolent.ts3j.musicbot.client;

import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

public class TeamspeakBot {
    private LocalTeamspeakClientSocket client;

    public TeamspeakBot() {

    }

    public LocalTeamspeakClientSocket getClient() {
        return client;
    }
}
