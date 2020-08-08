package com.vandendaelen.nicephore.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;

public class PlayerHelper {
    public static void sendMessage(ITextComponent message){
        Minecraft.getInstance().player.sendMessage(message, Minecraft.getInstance().player.getUniqueID());
    }
}
