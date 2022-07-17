package com.vandendaelen.nicephore;

import com.mojang.logging.LogUtils;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

@Mod(Nicephore.MODID)
public final class Nicephore {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "nicephore";
    public static final String MOD_NAME = "Nicephore";

    public Nicephore() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NicephoreConfig.CLIENT_SPEC);
        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }
}
