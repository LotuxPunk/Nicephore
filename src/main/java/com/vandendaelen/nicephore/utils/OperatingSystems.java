package com.vandendaelen.nicephore.utils;

import com.vandendaelen.nicephore.clipboard.ClipboardManager;
import com.vandendaelen.nicephore.clipboard.impl.WindowsClipboardManagerImpl;

public enum OperatingSystems {
    WINDOWS(WindowsClipboardManagerImpl.getInstance()),
    LINUX(null),
    MAC(null),
    SOLARIS(null);

    private ClipboardManager manager;
    private static OperatingSystems os;

    OperatingSystems(ClipboardManager instance) {
        this.manager = instance;
    }

    public static OperatingSystems getOS() {
        if (os == null) {
            final String operSys = System.getProperty("os.name").toLowerCase();
            if (operSys.contains("win")) {
                os = OperatingSystems.WINDOWS;
            } else if (operSys.contains("nix") || operSys.contains("nux")
                    || operSys.contains("aix")) {
                os = OperatingSystems.LINUX;
            } else if (operSys.contains("mac")) {
                os = OperatingSystems.MAC;
            } else if (operSys.contains("sunos")) {
                os = OperatingSystems.SOLARIS;
            }
        }
        return os;
    }

    public ClipboardManager getManager() {
        return manager;
    }
}
