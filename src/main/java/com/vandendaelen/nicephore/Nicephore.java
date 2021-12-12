package com.vandendaelen.nicephore;

import com.vandendaelen.nicephore.client.event.ClientEvents;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.thread.InitThread;
import com.vandendaelen.nicephore.utils.Util;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Nicephore.MODID)
public final class Nicephore {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "nicephore";
    public static final String MOD_NAME = "Nicephore";

    public Nicephore() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NicephoreConfig.CLIENT_SPEC);
        MinecraftForge.EVENT_BUS.register(this);

        if (Util.getOS() == Util.OS.WINDOWS){
            System.setProperty("java.awt.headless", "false");
        }

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,() -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        ClientEvents.init();

        final InitThread initThread = new InitThread(NicephoreConfig.Client.getOptimisedOutputToggle());
        initThread.start();
    }
}
