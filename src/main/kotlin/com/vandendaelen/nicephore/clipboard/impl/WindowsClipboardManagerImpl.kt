package com.vandendaelen.nicephore.clipboard.impl

import com.profesorfalken.jpowershell.PowerShell
import com.vandendaelen.nicephore.clipboard.ClipboardManager
import net.minecraft.Util
import java.io.File

class WindowsClipboardManagerImpl private constructor() : ClipboardManager {
    private val session: PowerShell? = if (Util.getPlatform() == Util.OS.WINDOWS) {
        PowerShell.openSession()
    } else {
        null
    }

    override fun clipboardImage(screenshot: File): Boolean {
        val command = """
            [Reflection.Assembly]::LoadWithPartialName('System.Drawing');
            [Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms');
            
            ${'$'}filename = "${screenshot.absolutePath}";
            ${'$'}file = get-item(${'$'}filename);
            ${'$'}img = [System.Drawing.Image]::Fromfile(${'$'}file);
            [System.Windows.Forms.Clipboard]::SetImage(${'$'}img);
        """.trimIndent()

        return session?.executeCommand(command)?.isError?.not() ?: false
    }

    companion object {
        val instance: WindowsClipboardManagerImpl by lazy { WindowsClipboardManagerImpl() }
    }
}
