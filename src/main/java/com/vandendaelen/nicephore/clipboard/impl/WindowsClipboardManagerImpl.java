package com.vandendaelen.nicephore.clipboard.impl;

import com.profesorfalken.jpowershell.PowerShell;
import com.vandendaelen.nicephore.clipboard.ClipboardManager;

import java.io.File;

public class WindowsClipboardManagerImpl implements ClipboardManager {
    private static WindowsClipboardManagerImpl INSTANCE;
    private final PowerShell session;

    public WindowsClipboardManagerImpl() {
        this.session = PowerShell.openSession();
    }

    @Override
    public void clipboardImage(File screenshot) {
        final String command = "[Reflection.Assembly]::LoadWithPartialName('System.Drawing');\n" +
                "[Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms');\n" +
                "\n" +
                String.format("$filename = \"%s\";\n", screenshot.getAbsolutePath()) +
                "$file = get-item($filename);\n" +
                "$img = [System.Drawing.Image]::Fromfile($file);\n" +
                "[System.Windows.Forms.Clipboard]::SetImage($img);";

        final Thread clipboardThread = new Thread(() -> session.executeCommand(command));
        clipboardThread.start();
    }

    public static WindowsClipboardManagerImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WindowsClipboardManagerImpl();
        }
        return INSTANCE;
    }
}
