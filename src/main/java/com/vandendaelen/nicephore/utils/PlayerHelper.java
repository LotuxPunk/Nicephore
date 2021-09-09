package com.vandendaelen.nicephore.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class PlayerHelper {
    public static void sendMessage(final Component message) {
        Minecraft mcInstance = Minecraft.getInstance();
        if (mcInstance.player != null) {
            mcInstance.player.displayClientMessage(message, false);
        }
    }

    public static void sendHotbarMessage(final Component message) {
        Minecraft mcInstance = Minecraft.getInstance();
        if (mcInstance.player != null) {
            mcInstance.player.displayClientMessage(message, true);
        }
    }

}
