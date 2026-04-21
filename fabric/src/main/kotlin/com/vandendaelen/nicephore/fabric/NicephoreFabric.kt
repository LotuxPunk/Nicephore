package com.vandendaelen.nicephore.fabric

import com.vandendaelen.nicephore.Nicephore
import com.vandendaelen.nicephore.NicephoreClient
import com.vandendaelen.nicephore.config.NicephoreConfigHolder
import com.vandendaelen.nicephore.fabric.platform.FabricScreenshotHook
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object NicephoreFabric : ClientModInitializer {
    override fun onInitializeClient() {
        Nicephore.LOGGER.info("NicephoreFabric initialising")
        // Eagerly initialise the config so file I/O happens before first tick.
        NicephoreConfigHolder.load()
        NicephoreClient.onInit()

        // Sanity-check: our Mixin target is a synthetic lambda method name which may
        // change across Minecraft versions. If the target isn't present we still boot
        // (mixins.json has required=false + defaultRequire=0), but the screenshot hook
        // silently becomes a no-op. Log a loud warning so the regression is visible.
        if (!FabricScreenshotHook.probeTargetMethodPresent()) {
            Nicephore.LOGGER.warn(
                "Screenshot capture hook is INACTIVE on Fabric: " +
                    "net.minecraft.client.Screenshot.lambda\$grab\$1 was not found " +
                    "(likely an MC update changed the lambda numbering). " +
                    "Custom screenshot message, clipboard copy, and oxipng optimisation " +
                    "will not trigger until the ScreenshotMixin target is updated."
            )
        }

        // Poll keybinds each client tick.
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            NicephoreClient.pollKeybindings()
        }
    }
}
