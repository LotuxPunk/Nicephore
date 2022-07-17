package com.vandendaelen.nicephore.client.event;

import com.vandendaelen.nicephore.Nicephore;
import com.vandendaelen.nicephore.client.KeyMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Nicephore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModBusEvents {

    @SubscribeEvent
    public static void onKeyMappingsRegistration(final RegisterKeyMappingsEvent event) {
        Arrays.stream(KeyMappings.values())
                .map(KeyMappings::getKey)
                .forEach(event::register);
    }
}
