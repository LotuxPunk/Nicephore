package com.vandendaelen.nicephore.enums;

import com.vandendaelen.nicephore.clipboard.ClipboardManager;
import com.vandendaelen.nicephore.clipboard.impl.MacOSClipboardManagerImpl;
import com.vandendaelen.nicephore.clipboard.impl.WindowsClipboardManagerImpl;
import net.minecraft.Util;

public enum OperatingSystems {
    WINDOWS(WindowsClipboardManagerImpl.getInstance()),
    LINUX(null),
    MAC(MacOSClipboardManagerImpl.getInstance()),
    SOLARIS(null);

    private final ClipboardManager manager;
    private static OperatingSystems OS;

    OperatingSystems(ClipboardManager instance) {
        this.manager = instance;
    }

    public static OperatingSystems getOS() {
        return switch (Util.getPlatform()) {
            case WINDOWS -> WINDOWS;
            case LINUX -> LINUX;
            case OSX -> MAC;
            case SOLARIS -> SOLARIS;
            default -> throw new IllegalStateException("Unexpected value: " + Util.getPlatform());
        };
    }

    public ClipboardManager getManager() {
        return manager;
    }
}
