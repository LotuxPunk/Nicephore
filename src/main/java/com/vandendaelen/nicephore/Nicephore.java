package com.vandendaelen.nicephore;

import com.vandendaelen.nicephore.event.ClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Nicephore.MODID)
public class Nicephore {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "nicephore";
    public static final String MOD_NAME = "Nicephore";

    public Nicephore() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.register(this);
        System.setProperty("java.awt.headless", "false");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        ClientEvents.init();
    }
}
