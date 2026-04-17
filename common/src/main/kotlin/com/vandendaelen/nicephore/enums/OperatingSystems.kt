package com.vandendaelen.nicephore.enums

import com.vandendaelen.nicephore.clipboard.ClipboardManager
import com.vandendaelen.nicephore.clipboard.impl.MacOSClipboardManagerImpl
import com.vandendaelen.nicephore.clipboard.impl.WindowsClipboardManagerImpl

enum class OperatingSystems(val manager: ClipboardManager?) {
    WINDOWS(WindowsClipboardManagerImpl.instance),
    LINUX(null),
    MAC(MacOSClipboardManagerImpl.instance),
    SOLARIS(null);

    companion object {
        fun getOS(): OperatingSystems {
            val osName = System.getProperty("os.name", "").lowercase()
            return when {
                osName.contains("win") -> WINDOWS
                osName.contains("mac") || osName.contains("darwin") -> MAC
                osName.contains("sunos") || osName.contains("solaris") -> SOLARIS
                else -> LINUX
            }
        }
    }
}
