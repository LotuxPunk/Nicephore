package com.vandendaelen.nicephore.utils;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Util {
    public enum OS {
        WINDOWS, LINUX, MAC, SOLARIS
    };

    private static OS os = null;

    public static OS getOS() {
        if (os == null) {
            final String operSys = System.getProperty("os.name").toLowerCase();
            if (operSys.contains("win")) {
                os = OS.WINDOWS;
            } else if (operSys.contains("nix") || operSys.contains("nux")
                    || operSys.contains("aix")) {
                os = OS.LINUX;
            } else if (operSys.contains("mac")) {
                os = OS.MAC;
            } else if (operSys.contains("sunos")) {
                os = OS.SOLARIS;
            }
        }
        return os;
    }

    public static DynamicTexture fileToTexture(File file) {
        NativeImage nativeImage = null;
        try {
            InputStream is = new FileInputStream(file);
            nativeImage = NativeImage.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DynamicTexture(nativeImage);
    }

    public static <T> Stream<List<T>> batches(List<T> source, int length) {
        if (length <= 0)
            throw new IllegalArgumentException("length = " + length);
        int size = source.size();
        if (size <= 0)
            return Stream.empty();
        int fullChunks = (size - 1) / length;
        return IntStream.range(0, fullChunks + 1).mapToObj(
                n -> source.subList(n * length, n == fullChunks ? size : (n + 1) * length));
    }
}
