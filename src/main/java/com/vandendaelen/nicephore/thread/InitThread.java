package com.vandendaelen.nicephore.thread;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.Reference;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class InitThread extends Thread {
    private static final File
            DESTINATION = new File(Minecraft.getInstance().gameDir.getAbsolutePath(), "mods\\nicephore"),
            REFERENCES_JSON = new File(DESTINATION,"\\references.json"),
            OXIPNG_ZIP = new File(DESTINATION,"\\oxipng.zip"),
            ECT_ZIP = new File(DESTINATION,"\\ect.zip");

    @Override
    public void run() {
        if (NicephoreConfig.Client.getOptimisedOutputToggle()) {
            // first read the old JSON if present
            {
                final Gson gson = new Gson();
                JsonReader reader = null;
                try {
                    reader = new JsonReader(new FileReader(REFERENCES_JSON));
                } catch (final IOException e) {
                    e.printStackTrace();
                }

                Type collectionType = new TypeToken<Collection<Response>>() {}.getType();
                Collection<Response> responses = gson.fromJson(reader, collectionType);
                Optional<Response> response = responses.stream().filter(response1 -> response1.platform.equals(Util.getOS().name())).findFirst();

                if (response.isPresent()) {
                    if (!Files.exists(DESTINATION.toPath())) {
                        try {
                            Files.createDirectory(DESTINATION.toPath());
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }

                    downloadAndExtract(response.get().oxipng, OXIPNG_ZIP);
                    downloadAndExtract(response.get().ect, ECT_ZIP);

                    Reference.Command.OXIPNG = response.get().oxipng_command;
                    Reference.Command.ECT = response.get().ect_command;

                    Reference.File.OXIPNG = response.get().oxipng_file;
                    Reference.File.ECT = response.get().ect_file;

                    Reference.Version.OXIPNG = response.get().oxipng_version;
                    Reference.Version.ECT = response.get().ect_version;
                }
            }

            // now get the new one
            {
                final Gson gson = new Gson();
                JsonReader reader = null;
                try {
                    FileUtils.copyURLToFile(new URL(Reference.DOWNLOADS_URLS), REFERENCES_JSON); // download the new one
                    reader = new JsonReader(new FileReader(REFERENCES_JSON));
                } catch (final IOException e) {
                    e.printStackTrace();
                }

                Type collectionType = new TypeToken<Collection<Response>>() {}.getType();
                Collection<Response> responses = gson.fromJson(reader, collectionType);
                Optional<Response> response = responses.stream().filter(response1 -> response1.platform.equals(Util.getOS().name())).findFirst();

                if (response.isPresent()) {
                    if (!Files.exists(DESTINATION.toPath())) {
                        try {
                            Files.createDirectory(DESTINATION.toPath());
                        } catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // only download and extract if the new JSON version is different to the old one
                    if (!Reference.Version.OXIPNG.equals(response.get().oxipng_version)) {
                        Reference.Version.OXIPNG = response.get().oxipng_version;
                        downloadAndExtract(response.get().oxipng, OXIPNG_ZIP);
                    }

                    if (!Reference.Version.ECT.equals(response.get().ect_version)) {
                        Reference.Version.ECT = response.get().ect_version;
                        downloadAndExtract(response.get().ect, ECT_ZIP);
                    }
                }
            }
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

            unzip(zip.getAbsolutePath(), DESTINATION.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void unzip(String zipFilePath, String destDir) {
        final File dir = new File(destDir);
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs();
        final FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to "+newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                final FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
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
