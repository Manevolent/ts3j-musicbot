package com.github.manevolent.ts3j.musicbot.audio.mixer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MixerProcessTask implements Runnable {
    private final Mixer mixer;
    private final double runningFrequency, stoppedFrequency;

    public MixerProcessTask(Mixer mixer, double runningFrequency, double stoppedFrequency) {
        this.mixer = mixer;
        this.runningFrequency = runningFrequency;
        this.stoppedFrequency = stoppedFrequency;
    }

    @Override
    public void run() {
        double runningNanosecondInterval = (1_000_000_000D / runningFrequency);
        double stoppedNanosecondInterval = (1_000_000_000D / stoppedFrequency);

        long wake = System.nanoTime();

        while (true) {
            if (mixer.isPlaying()) {
                try {
                    if (!mixer.isRunning())
                        mixer.setRunning(true);

                    mixer.processBuffer();
                } catch (Throwable e) {
                    Logger.getGlobal().log(Level.SEVERE, "Problem processing mixer task", e);
                    return;
                }

                wake += runningNanosecondInterval;
            } else {
                if (mixer.isRunning())
                    mixer.setRunning(false);

                wake += stoppedNanosecondInterval;
            }

            long sleep = wake - System.nanoTime();
            if (sleep > 0)
                try {
                    Thread.sleep(sleep / 1_000_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
        }
    }
}
