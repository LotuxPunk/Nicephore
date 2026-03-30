package com.vandendaelen.nicephore.enums

import com.vandendaelen.nicephore.clipboard.ClipboardManager
import com.vandendaelen.nicephore.clipboard.impl.MacOSClipboardManagerImpl
import com.vandendaelen.nicephore.clipboard.impl.WindowsClipboardManagerImpl
import net.minecraft.Util

enum class OperatingSystems(val manager: ClipboardManager?) {
    WINDOWS(WindowsClipboardManagerImpl.instance),
    LINUX(null),
    MAC(MacOSClipboardManagerImpl.instance),
    SOLARIS(null);

    companion object {
        fun getOS(): OperatingSystems = when (Util.getPlatform()) {
            Util.OS.WINDOWS -> WINDOWS
            Util.OS.LINUX -> LINUX
            Util.OS.OSX -> MAC
            Util.OS.SOLARIS -> SOLARIS
            else -> throw IllegalStateException("Unexpected value: ${Util.getPlatform()}")
        }
    }
}
