package com.vandendaelen.nicephore;

import com.mojang.logging.LogUtils;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Nicephore.MODID)
public final class Nicephore {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "nicephore";
    public static final String MOD_NAME = "Nicephore";

    public Nicephore(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, NicephoreConfig.CLIENT_SPEC);
        context.registerDisplayTest(IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION);
    }
}
