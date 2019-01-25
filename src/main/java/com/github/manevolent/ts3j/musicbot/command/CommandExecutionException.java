package com.github.manevolent.ts3j.musicbot.command;

public class CommandExecutionException extends Exception {
    public CommandExecutionException(String message) {
        super(message);
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandExecutionException(Throwable cause) {
        super(cause);
    }
}
