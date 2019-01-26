package com.github.manevolent.ts3j.musicbot.audio.player;

import com.github.manevolent.ts3j.musicbot.audio.mixer.input.MixerChannel;

public abstract class AudioPlayer implements MixerChannel {

    public abstract boolean isClosed();

    /**
     * Finds if the player is playing any audio.
     *
     * Must return true if any samples might be produced by this player.
     *
     * @return true if the player is playing any audio.
     */
    public abstract boolean isPlaying();

    /**
     * Stops the player softly.  In some implementations this may continuing playing samples (e.g. fade out.)
     *
     * @return true if the player will stop or begin to stop.
     */
    public abstract boolean stop();

    /**
     * Kills the audio player immediately, stopping all playback.
     *
     * @return true if the player was killed, false otherwise.
     */
    public boolean kill() {
        try {
            close();

            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
