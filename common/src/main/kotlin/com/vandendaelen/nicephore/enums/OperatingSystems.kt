package com.vandendaelen.nicephore.enums

import com.vandendaelen.nicephore.clipboard.ClipboardManager

enum class OperatingSystems {
    WINDOWS, LINUX, MAC, SOLARIS;

    /**
     * Resolved on demand so platform-specific implementation classes are only loaded when
     * this enum value is actually queried for its manager. Crucial on Fabric, where
     * WindowsClipboardManagerImpl references com.profesorfalken.jpowershell.PowerShell —
     * triggering that class to load on a non-Windows Fabric install would crash the JVM
     * with NoClassDefFoundError.
     */
    val manager: ClipboardManager?
        get() = when (this) {
            WINDOWS -> com.vandendaelen.nicephore.clipboard.impl.WindowsClipboardManagerImpl.instance
            MAC -> com.vandendaelen.nicephore.clipboard.impl.MacOSClipboardManagerImpl.instance
            LINUX, SOLARIS -> null
        }

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
