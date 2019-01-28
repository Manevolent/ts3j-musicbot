package com.github.manevolent.ts3j.musicbot.client;

import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.musicbot.audio.mixer.BufferedMixer;
import com.github.manevolent.ts3j.musicbot.audio.mixer.Mixer;
import com.github.manevolent.ts3j.musicbot.audio.mixer.MixerProcessTask;
import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.MixerFilter;
import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.type.FilterDither;
import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.type.FilterGain;
import com.github.manevolent.ts3j.musicbot.audio.mixer.filter.type.SoftClipFilter;
import com.github.manevolent.ts3j.musicbot.audio.opus.OpusParameters;
import com.github.manevolent.ts3j.musicbot.audio.player.AudioPlayer;
import com.github.manevolent.ts3j.musicbot.audio.player.ResampledAudioPlayer;
import com.github.manevolent.ts3j.musicbot.audio.resample.FFmpegResampler;
import com.github.manevolent.ts3j.musicbot.config.ConfigurationHelper;
import com.github.manevolent.ts3j.musicbot.download.DownloadManager;
import com.github.manevolent.ts3j.musicbot.download.DownloadSource;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeamspeakBot implements TS3Listener {
    private final JsonObject defaultConfiguration;
    private final JsonObject serverConfiguration;

    private final LocalTeamspeakClientSocket client;
    private final Mixer mixer;

    private final Callable<LocalIdentity> identityCallable;
    private final Consumer<Float> volumeControl;

    private final DownloadManager downloadManager;

    private boolean running = false;

    public TeamspeakBot(JsonObject defaultConfiguration, JsonObject serverConfiguration) {
        this.defaultConfiguration = defaultConfiguration;
        this.serverConfiguration = serverConfiguration;

        this.client = new LocalTeamspeakClientSocket();
        this.client.addListener(this);

        // Nickname
        client.setNickname(
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "bot.nickname").getAsString()
        );

        // HWID
        client.setHWID(
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "bot.hwid").getAsString()
        );

        // Identity
        Callable<LocalIdentity> identityCallable;
        String identityType =
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "bot.identity.type")
                .getAsString();
        int identityLevel =
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "bot.identity.level").getAsInt();
        switch (identityType) {
            case "file":
                identityCallable = () -> {
                    File securityLevel = new File(
                            ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "bot.identity.file")
                            .getAsString()
                    );
                    LocalIdentity identity;

                    if (!securityLevel.exists()) {
                        identity = LocalIdentity.generateNew(identityLevel);
                        identity.save(securityLevel);
                    } else {
                        identity = LocalIdentity.read(securityLevel);
                    }

                    return identity;
                };
                break;
            default:
                throw new IllegalArgumentException("unknown identity type: " + identityType);
        }
        this.identityCallable = identityCallable;

        try {
            client.setIdentity(identityCallable.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Create a new buffered mixer, which mixes many sources of audio.
        float sampleRate = 48000F;
        int channels = 2;
        float bufferSizePerChannel =
                sampleRate *
                ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "opus.buffer").getAsFloat();
        int bufferSize = (int) (bufferSizePerChannel * channels);
        this.mixer = new BufferedMixer(bufferSize, sampleRate, channels);

        // < Filter chain >
        // Mine is rather basic. You could add EQs, limiters, stereo separation, light reverberation, etc. here
        // This gives a decent sound regardless of volume, and protects against clipping/distortions.
        // See the respective Filter java files for more information.
        {
            // Add a gain/volume filter, and attach the filter's control to the volumeControl function.
            FilterGain gainFilter = new FilterGain(1f);
            MixerFilter[] multichannelGainFilter = new MixerFilter[channels];
            for (int ch = 0; ch < channels; ch++) multichannelGainFilter[ch] = gainFilter;
            this.volumeControl = gainFilter::setQ;
            this.mixer.addFilter(multichannelGainFilter);

            // Add a soft-clip filter, which protects the output of the mixer from going beyond the limit of [-1,1]
            MixerFilter[] multichannelClipFilter = new MixerFilter[channels];
            for (int ch = 0; ch < channels; ch++) multichannelClipFilter[ch] = new SoftClipFilter();
            this.mixer.addFilter(multichannelClipFilter);

            // Add a dither filter, which inserts noise to reduce quantization/rounding errors with 24-bit audio.
            // This may be unnecessary on our 32-bit audio processing pipeline, not to mention OPUS probably doesn't
            // care, but I am adding it anyways since most DACs on the receiving end are 24-bit at most.
            MixerFilter[] multichannelDitherFilter = new MixerFilter[channels];
            for (int ch = 0; ch < channels; ch++) multichannelDitherFilter[ch] = new FilterDither(24);
            this.mixer.addFilter(multichannelDitherFilter);
        }

        // Create a sink, which is used to move raw audio samples into Teamspeak over a packet buffer (rather efficient)
        TeamspeakFastMixerSink sink = new TeamspeakFastMixerSink(
                new AudioFormat(sampleRate, 32, channels, true, false),
                bufferSize * 4,
                OpusParameters.fromJson(defaultConfiguration, serverConfiguration)
        );

        // Add the sink into the mixer so that samples may flush down into this sink.
        this.mixer.addSink(sink);

        Thread mixerThread = new Thread(new MixerProcessTask(mixer, 20D, 2D));
        mixerThread.start();

        // Set the TS3J microphone so that the client can send the audio data to the Teamspeak3 server.
        client.setMicrophone(sink);

        // Set up downloads
        downloadManager = new DownloadManager();
        JsonArray sources = ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "sources")
                .getAsJsonArray();

        for (JsonElement sourceElement : sources) {
            JsonObject sourceObject = sourceElement.getAsJsonObject();
            String className = sourceObject.get("class").getAsString();

            try {
                Class<? extends DownloadSource> clazz =
                        (Class<? extends DownloadSource>) ClassLoader.getSystemClassLoader().loadClass(className);

                downloadManager.registerSource(clazz.getConstructor(JsonObject.class).newInstance(sourceObject));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("invalid source class", e);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public LocalTeamspeakClientSocket getClient() {
        return client;
    }

    public void setRunning(boolean running) {
        if (this.running != running) {

            if (running) {
                onStart();
            } else {
                onStop();
            }

            this.running = running;

        }
    }

    private void onStart() {
        while (true) {
            try {
                if (ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "password").isJsonNull()) {
                    client.connect(
                            ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "address").getAsString(),
                            ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "timeout").getAsInt()
                    );
                } else {
                    client.connect(
                            ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "address").getAsString(),
                            ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "password").getAsString(),
                            ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "timeout").getAsInt()
                    );
                }

                client.waitForState(
                        ClientConnectionState.CONNECTED,
                        ConfigurationHelper.get(defaultConfiguration, serverConfiguration, "timeout").getAsInt()
                );

                break;
            } catch (Exception e) {
                Logger.getGlobal().log(
                        Level.WARNING,
                        "Failed to connect to " +
                                ConfigurationHelper.get(
                                        defaultConfiguration,
                                        serverConfiguration,
                                        "address"
                                ).getAsString(),
                        e
                );
            }
        }
    }

    private void onStop() {
        try {
            client.disconnect();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    public void onTextMessage(TextMessageEvent e) {
        String message = e.getMessage();

        message = message
                .replace("[URL]", "")
                .replace("[/URL]", "")
                .split("\n")[0]
                .split("\r")[0]
                .trim();

        if (message.startsWith("!") && message.length() > 1) {
            message = message.substring(1);
        } else return;

        String[] parts = message.split("\\s", 2);
        String label = parts[0].toLowerCase();
        String arguments = parts[1];

        try {
            switch (label) {
                case "v":
                    AudioPlayer player = downloadManager.get(URI.create(arguments)).openPlayer();
                    mixer.addChannel(
                            new ResampledAudioPlayer(
                                    player,
                                    new FFmpegResampler(
                                            new AudioFormat(player.getSampleRate(), 32, player.getChannels(), true, false),
                                            new AudioFormat(mixer.getAudioSampleRate(), 32, mixer.getAudioChannels(), true, false),
                                            mixer.getBufferSize()
                                    ),
                                    mixer.getBufferSize()
                            )
                    );
                    break;
            }
        } catch (Exception ex) {
            Logger.getGlobal().log(Level.WARNING, "Exception handling message", ex);

            try {
                client.sendServerMessage(ex.getMessage());
            } catch (Exception e1) {
                Logger.getGlobal().log(Level.SEVERE, "Problem replying exception", e1);
            }
        }
    }

    @Override
    public void onClientJoin(ClientJoinEvent e) {

    }

    @Override
    public void onClientLeave(ClientLeaveEvent e) {

    }

    @Override
    public void onClientMoved(ClientMovedEvent e) {

    }

    @Override
    public void onConnected(ConnectedEvent e) {

    }

    @Override
    public void onDisconnected(DisconnectedEvent e) {

    }
}
