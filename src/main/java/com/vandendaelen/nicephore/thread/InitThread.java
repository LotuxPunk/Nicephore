package com.vandendaelen.nicephore.thread;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.vandendaelen.nicephore.utils.Reference;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FileUtils;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

public final class InitThread extends Thread {

    private boolean optimiseConfig;

    private static final File
            DESTINATION = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath(), String.format("mods%snicephore", File.separator)),
            REFERENCES_JSON = new File(DESTINATION, String.format("%sreferences.json", File.separator));
    public InitThread(boolean optimiseConfig) {
        this.optimiseConfig = optimiseConfig;
    }

    @Override
    public void run() {
        if (optimiseConfig) {
            if (Files.exists(DESTINATION.toPath())) {
                {
                    try {
                        Optional<Response> response = getResponse(getJsonReader(REFERENCES_JSON));

                        if (response.isPresent()) {
                            Reference.Command.OXIPNG = response.get().oxipng_command;
                            Reference.Command.ECT = response.get().ect_command;

                            Reference.File.OXIPNG = response.get().oxipng_file;
                            Reference.File.ECT = response.get().ect_file;

                            Reference.Version.OXIPNG = response.get().oxipng_version;
                            Reference.Version.ECT = response.get().ect_version;
                        }
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }

                {
                    try {
                        final Optional<Response> response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON));

                        if (response.isPresent()) {
                            if (!Reference.Version.OXIPNG.equals(response.get().oxipng_version)) {
                                Reference.Version.OXIPNG = response.get().oxipng_version;
                                downloadAndExtract(response.get().oxipng);
                            }

                            if (!Reference.Version.ECT.equals(response.get().ect_version)) {
                                Reference.Version.ECT = response.get().ect_version;
                                downloadAndExtract(response.get().ect);
                            }
                        }
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                freshInstall();
            }
        }
    }

    private static void downloadAndExtract(String urlString){

        URL url = null;
        String filename = "";

        try {
            url = new URL(urlString);
            filename = Paths.get(url.getPath()).getFileName().toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final File zip = new File(DESTINATION, String.format("%s%s", File.separator, filename));

        if (!filename.isEmpty()) {
            try (BufferedInputStream in = new BufferedInputStream(url.openStream()); FileOutputStream fileOutputStream = new FileOutputStream(zip)) {
                byte dataBuffer[] = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }

                Archiver archiver = ArchiverFactory.createArchiver(zip);
                archiver.extract(zip, DESTINATION);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Optional<Response> getResponse(final JsonReader reader) {
        final Gson gson = new Gson();
        final Type collectionType = new TypeToken<Collection<Response>>() {}.getType();
        final Collection<Response> responses = gson.fromJson(reader, collectionType);
        final Optional<Response> response = responses.stream().filter(response1 -> response1.platform.equals(Util.getOS().name())).findFirst();
        return response;
    }

    private JsonReader getJsonReader(String URL, final File file) throws IOException {
        FileUtils.copyURLToFile(new URL(URL), file);
        return getJsonReader(file);
    }

    private JsonReader getJsonReader(final File file) throws FileNotFoundException {
        return new JsonReader(new FileReader(file));
    }

    private void freshInstall() {
        try {
            Files.createDirectory(DESTINATION.toPath());
            final Optional<Response> response = getResponse(getJsonReader(Reference.DOWNLOADS_URLS, REFERENCES_JSON));

            Reference.Command.OXIPNG = response.get().oxipng_command;
            Reference.Command.ECT = response.get().ect_command;

            Reference.File.OXIPNG = response.get().oxipng_file;
            Reference.File.ECT = response.get().ect_file;

            Reference.Version.OXIPNG = response.get().oxipng_version;
            Reference.Version.ECT = response.get().ect_version;

            downloadAndExtract(response.get().oxipng);
            downloadAndExtract(response.get().ect);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class Response{
        String platform;
        String oxipng, oxipng_file, oxipng_command, oxipng_version;
        String ect, ect_file, ect_command, ect_version;
    }
}
