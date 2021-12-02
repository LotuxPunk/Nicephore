package com.vandendaelen.nicephore.utils;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
}
