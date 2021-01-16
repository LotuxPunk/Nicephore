package com.vandendaelen.nicephore.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

public final class PlayerHelper {
    public static void sendMessage(final ITextComponent message) {
        Minecraft mcInstance = Minecraft.getInstance();
        mcInstance.player.sendMessage(message, mcInstance.player.getUniqueID());
    }

    public static void sendHotbarMessage(final ITextComponent message) {
        Minecraft mcInstance = Minecraft.getInstance();
        if (mcInstance.player != null) {
            mcInstance.player.sendStatusMessage(message, true);
        }
    }

}
