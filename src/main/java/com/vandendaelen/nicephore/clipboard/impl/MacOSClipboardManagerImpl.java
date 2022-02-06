package com.vandendaelen.nicephore.clipboard.impl;

import com.vandendaelen.nicephore.clipboard.ClipboardManager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MacOSClipboardManagerImpl implements ClipboardManager {

    private static MacOSClipboardManagerImpl INSTANCE;

    @Override
    public boolean clipboardImage(File screenshot) {
        final String[] components = screenshot.getName().split("\\.");
        final long count = components.length;
        final String extension = Arrays.stream(components).skip(count - 1).findFirst().get();

        final String[] cmd = { "osascript", "-e", String.format("'set the clipboard to (read (POSIX file \"%s\") as %s picture)'", screenshot.getAbsolutePath(), extension.toUpperCase())};

        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static MacOSClipboardManagerImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MacOSClipboardManagerImpl();
        }
        return INSTANCE;
    }
}
