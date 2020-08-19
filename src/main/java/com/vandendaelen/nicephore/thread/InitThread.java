package com.vandendaelen.nicephore.thread;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.Reference;
import com.vandendaelen.nicephore.utils.Util;
import net.lingala.zip4j.ZipFile;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

public class InitThread extends Thread {
    private static final File DESTINATION = new File(Minecraft.getInstance().gameDir.getAbsolutePath(), "mods\\nicephore");
    private static final File OXIPNG_ZIP = new File(DESTINATION,"\\oxipng.zip");
    private static final File ECT_ZIP = new File(DESTINATION,"\\ect.zip");

    @Override
    public void run() {
        Gson gson = new Gson();

        String json = null;
        try {
            json = readUrl(Reference.DOWNLOADS_URLS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Type collectionType = new TypeToken<Collection<Response>>(){}.getType();
        Collection<Response> responses = gson.fromJson(json, collectionType);

        if (Util.getOS() == Util.OS.WINDOWS){
            Optional<Response> response = responses.stream().filter(response1 -> response1.version.equals(Reference.VERSION)).findFirst();
            if(response.isPresent() && NicephoreConfig.Client.getOptimisedOutputToggle()){
                Path path = Paths.get(DESTINATION.getAbsolutePath());

                if (!Files.exists(path)) {
                    try {
                        Files.createDirectory(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    downloadAndExtract(response.get().oxipng, OXIPNG_ZIP);
                    downloadAndExtract(response.get().ect, ECT_ZIP);
                }
            }
        }
    }

    private static String readUrl(String urlString) throws Exception {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }

    }

    private static void downloadAndExtract(String url, File zip){
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(zip)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }

            ZipFile zipFile = new ZipFile(zip);
            zipFile.extractAll(DESTINATION.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Response{
        String version;
        String oxipng;
        String ect;
    }
}
