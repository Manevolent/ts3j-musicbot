package com.github.manevolent.ts3j.musicbot.download;

import com.github.manevolent.ffmpeg4j.FFmpeg;
import com.github.manevolent.ffmpeg4j.FFmpegException;
import com.github.manevolent.ts3j.musicbot.audio.player.AudioPlayer;
import com.github.manevolent.ts3j.musicbot.audio.player.FFmpegAudioPlayer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bytedeco.javacpp.avformat;

import java.io.*;
import java.net.*;
import java.util.*;

public class YoutubeDLSource extends AbstractDownloadSource {
    private final File youtubeDlExecutable;

    private final Collection<String> allowedHosts;

    private RequestMode requestMode = RequestMode.FORCE_HTTP; // Caching

    public YoutubeDLSource(JsonObject configuration) {
        super("youtube-dl");

        this.youtubeDlExecutable = new File(configuration.get("exec").getAsString());

        List<String> allowedHosts = new LinkedList<>();
        for (JsonElement allowedHostElement : configuration.get("allowed-hosts").getAsJsonArray()) {
            allowedHosts.add(allowedHostElement.getAsString().toLowerCase());
        }

        this.allowedHosts = Collections.unmodifiableCollection(allowedHosts);
    }

    @Override
    public boolean canDownload(URI uri) {
        return uri.getScheme() != null &&
                (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"));
    }


    /**
     * Grabs JSON metadata from the specified video
     * @param videoUrl Video's URL
     * @return JSON metadata
     * @throws IOException
     */
    private JsonObject getJson(URL videoUrl) throws IOException {
        // Using an empty list would get around this check
        if (allowedHosts.size() > 0 && !allowedHosts.contains(videoUrl.getHost().toLowerCase()))
            throw new IOException(new IllegalAccessException("host not allowed: " + videoUrl.getHost()));

        if (!youtubeDlExecutable.exists())
            throw new IOException(new FileNotFoundException(youtubeDlExecutable.getAbsolutePath() + " does not exist"));

        // -4: Force IPv4, this is necessary for some VM/dedi hosts, which YouTube blocks IPv6 for.
        // --no-warnings: keep clutter off of stderr.
        // -j: JSON metadata ONLY; no download.
        Process process = Runtime.getRuntime().exec(
                new String[]{
                        youtubeDlExecutable.getAbsolutePath(),
                        "-4",
                        "--no-warnings",
                        "-j",
                        videoUrl.toExternalForm()
                }
        );

        try {
            process.getOutputStream().close();

            StringBuilder errorBuilder = new StringBuilder();

            if (process.getErrorStream().available() > 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String s = null;

                    while (reader.ready()) {
                        String s_ = reader.readLine();
                        if (s == null) s = s_;
                        errorBuilder.append(s).append(" ");
                    }

                    throw new IOException(errorBuilder.toString());
                }
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) builder.append(line);
                return (JsonObject) new JsonParser().parse(builder.toString());
            }
        } finally {
            // don't leave/leak hanging processes
            if (process.isAlive()) process.destroy();
        }
    }

    @Override
    public com.github.manevolent.ts3j.musicbot.download.Download get(URI uri) throws IOException {
        URL url = uri.toURL();

        // force HTTP for caching reasons

        switch (requestMode) {
            case FORCE_HTTP:
                if (url.getProtocol().equalsIgnoreCase("https"))
                    url = new URL("http", url.getHost(), url.getPort(), url.getFile());
                break;
            case FORCE_HTTPS:
                if (url.getProtocol().equalsIgnoreCase("http"))
                    url = new URL("https", url.getHost(), url.getPort(), url.getFile());
                break;
        }

        // Get a proper format to download (best-audio)
        JsonObject jsonObject = getJson(url);

        List<FormatOption> formatOptions = new LinkedList<>();

        if (!jsonObject.has("formats") || jsonObject.get("formats").isJsonNull()) {
            if (jsonObject.get("direct").getAsBoolean()) {
                FormatOption formatOption = new FormatOption(
                        url,
                        0L,
                        0D,
                        0D,
                        jsonObject.get("ext").getAsString(),
                        "direct",
                        null,
                        null
                );

                if (jsonObject.has("http_headers"))
                    jsonObject.getAsJsonObject("http_headers").entrySet()
                            .forEach(y -> formatOption.headers.put(y.getKey(), y.getValue().getAsString()));

                formatOptions.add(formatOption);
            } else {
                throw new IOException("JSON result has no \"formats\" property");
            }
        } else {
            jsonObject.get("formats").getAsJsonArray().forEach((x) -> {
                if (x == null || !x.isJsonObject()) return;

                JsonObject object = x.getAsJsonObject();
                if (!object.has("url") || object.get("url").isJsonNull()) return;

                try {
                    FormatOption formatOption = new FormatOption(
                            URI.create(object.get("url").getAsString().trim()).toURL(),
                            object.has("filesize") && !object.get("filesize").isJsonNull() ?
                                    object.get("filesize").getAsLong() : 0,
                            object.has("abr") && !object.get("abr").isJsonNull() ?
                                    object.get("abr").getAsDouble() : 0D,
                            object.has("vbr") && !object.get("vbr").isJsonNull() ?
                                    object.get("vbr").getAsDouble() : 0D,
                            object.has("ext") && !object.get("ext").isJsonNull() ?
                                    object.get("ext").getAsString().trim() : null,
                            object.has("format_note") && !object.get("format_note").isJsonNull() ?
                                    object.get("format_note").getAsString().trim() : null,
                            object.has("acodec") && !object.get("acodec").isJsonNull() ?
                                    object.get("acodec").getAsString().trim() : null,
                            object.has("vcodec") && !object.get("vcodec").isJsonNull() ?
                                    object.get("vcodec").getAsString().trim() : null
                    );

                    if (object.has("http_headers"))
                        object.getAsJsonObject("http_headers").entrySet()
                                .forEach(y -> formatOption.headers.put(y.getKey(), y.getValue().getAsString()));

                    formatOptions.add(formatOption);
                } catch (MalformedURLException e) {
                    // continue
                }
            });
        }

        Map<String, String> metadata = new HashMap<>();

        // Title
        metadata.put("title", jsonObject.has("fulltitle") ?
                jsonObject.get("fulltitle").getAsString() :
                jsonObject.get("title").getAsString());

        // Thumbnail URL
        if (jsonObject.has("thumbnail"))
            metadata.put("thumbnail", jsonObject.get("thumbnail").getAsString());

        // Duration (seconds)
        if (jsonObject.has("duration"))
            metadata.put("duration", Integer.toString(jsonObject.get("duration").getAsInt()));



        return new Download(
                Collections.unmodifiableMap(metadata),
                this,
                Collections.unmodifiableList(formatOptions)
        );
    }

    private static FormatOption getBestFormat(Collection<FormatOption> formatOptions) {
        FormatOption selectedFormat = null;

        if (formatOptions.size() == 1)
            selectedFormat = formatOptions.stream().findFirst().orElse(null);
        else if (formatOptions.size() > 1) {
            // We want the highest bitrate, but the lowest filesize.
            // YouTube throttles the DASH audio options so we totally exclude those from the results
            // Then, we look for options which explicitly provide an audio codec and a bitrate for that codec
            // And then, we descend by abr, then ascend by the file size
            // First option is the video we want
            selectedFormat = formatOptions
                    .stream()
                    .filter(x -> x.getNote() == null || !x.getNote().equals("DASH audio"))
                    .filter(x -> x.getAudioBitrate() > 0)
                    .sorted(((Comparator<FormatOption>)
                            (a,b) -> Double.compare(b.getAudioBitrate(), a.getAudioBitrate()))
                            .thenComparingLong(FormatOption::getFilesize))
                    .findFirst().orElse(null);
        }

        if (selectedFormat == null)
            throw new IllegalArgumentException("no suitable format of " + formatOptions.size() + " options");

        return selectedFormat;
    }

    public RequestMode getRequestMode() {
        return requestMode;
    }

    public void setRequestMode(RequestMode requestMode) {
        this.requestMode = requestMode;
    }

    private static class FormatOption {
        private final double audio_bitrate;
        private final double video_bitrate;

        private final long filesize;

        private final double audio_efficiency;
        private final double video_efficiency;

        private final URL url;
        private final String extension,note,audioCodec,videoCodec;

        private final Map<String, String> headers = new HashMap<>();

        FormatOption(URL url,
                     long filesize,
                     double audio_bitrate, double video_bitrate,
                     String extension, String note,
                     String audioCodec, String videoCodec) {
            this.audio_bitrate = audio_bitrate;
            this.video_bitrate = video_bitrate;

            this.filesize = filesize;
            this.extension = extension;
            this.audioCodec = audioCodec;
            this.videoCodec = videoCodec;

            if (filesize <= 0) {
                this.audio_efficiency = this.video_efficiency = 0;
            } else {
                this.audio_efficiency = filesize > 0 ? audio_bitrate / (filesize * 8) : 0D;
                this.video_efficiency = filesize > 0 ? video_bitrate / (filesize * 8) : 0D;
            }

            this.url = url;
            this.note = note;
        }

        public double getAudioBitrate() {
            return audio_bitrate;
        }
        public double getVideoBitrate() {
            return video_bitrate;
        }
        public double getAudioEfficiency() {
            return audio_efficiency;
        }
        public double getVideoEfficiency() {
            return video_efficiency;
        }
        public long getFilesize() {
            return filesize;
        }

        public URL getUrl() {
            return url;
        }

        public String getExtension() {
            return extension;
        }

        public String getNote() {
            return note;
        }

        public String getAudioCodec() {
            return audioCodec;
        }

        public String getVideoCodec() {
            return videoCodec;
        }
    }

    private class Download extends AbstractDownload {
        private final List<FormatOption> formatOptions;

        public Download(Map<String, String> metadata,
                        DownloadSource source,
                        List<FormatOption> formatOptions) {
            super(metadata, source);

            this.formatOptions = formatOptions;
        }

        @Override
        public AudioPlayer openPlayer() throws IOException {
            FormatOption selectedFormat = getBestFormat(formatOptions);

            HttpURLConnection urlConnection = (HttpURLConnection) selectedFormat.getUrl().openConnection();

            urlConnection.setRequestMethod("GET");

            for (Map.Entry<String, String> header : selectedFormat.headers.entrySet())
                urlConnection.setRequestProperty(header.getKey(), header.getValue());

            int responseCode = urlConnection.getResponseCode();
            if (responseCode / 100 != 2)
                throw new IOException("Unexpected response code from URL=" +
                        selectedFormat.getUrl().toExternalForm() +
                        ": " + responseCode);

            // Get MIME type
            String mimeType = null;
            if (selectedFormat.getExtension() != null)
                mimeType = URLConnection.guessContentTypeFromName("example." + selectedFormat.getExtension());

            if (mimeType == null || mimeType.length() <= 0)
                mimeType = urlConnection.getContentType();

            avformat.AVInputFormat inputFormat;

            try {
                inputFormat = FFmpeg.getInputFormatByName(selectedFormat.getExtension());
            } catch (FFmpegException e) {
                try {
                    inputFormat = FFmpeg.getInputFormatByMime(mimeType);
                } catch (FFmpegException e2) {
                    throw new IOException(e2);
                }
            }

            // not reached, but just in case:
            if (inputFormat == null)
                throw new IOException(new FFmpegException(
                        "unknown container format: " + selectedFormat.getExtension().toUpperCase()
                                + " (mime=" + mimeType + ")"
                ));

            try {
                return FFmpegAudioPlayer.open(
                        inputFormat,
                        urlConnection.getInputStream(),
                        10240
                );
            } catch (FFmpegException e) {
                throw new IOException(e);
            }
        }
    }

    public enum RequestMode {
        KEEP_SCHEME,
        FORCE_HTTPS,
        FORCE_HTTP
    }
}
