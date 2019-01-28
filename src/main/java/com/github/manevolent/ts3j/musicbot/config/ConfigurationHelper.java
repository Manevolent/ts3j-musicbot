package com.github.manevolent.ts3j.musicbot.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ConfigurationHelper {
    public static JsonElement get(JsonObject object, String path) {
        JsonElement element = object;
        for (String node : path.split("\\.")) {
            if (!element.getAsJsonObject().has(node)) throw new IllegalArgumentException();
            element = element.getAsJsonObject().get(node);
        }
        return element;
    }

    public static JsonElement get(JsonObject defaultObject, JsonObject specificObject, String path) {
        try {
            return get(specificObject, path);
        } catch (IllegalArgumentException ex) {
            return get(defaultObject, path);
        }
    }
}
