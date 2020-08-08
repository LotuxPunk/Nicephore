package com.vandendaelen.nicephore.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class NicephoreConfig {
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Client, ForgeConfigSpec> specClientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specClientPair.getRight();
        CLIENT = specClientPair.getLeft();
    }

    public static class Client {
        public final ForgeConfigSpec.DoubleValue compression;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("Client settings");
            compression = builder
                    .comment("Compression level")
                    .defineInRange("compression", 0.9, 0.0,1.0);
            builder.pop();
        }
        public static float getCompressionLevel() {
            return CLIENT.compression.get().floatValue();
        }
    }
}
