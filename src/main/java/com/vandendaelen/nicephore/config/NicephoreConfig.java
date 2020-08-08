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
        public final ForgeConfigSpec.BooleanValue makeJPEGs;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("Client settings");
            compression = builder
                    .comment("JPEG compression level, the higher the number, the better the quality." +
                            "\r\nNote that 1.0 is *not* lossless as JPEG is a lossy-only format, use the PNG files instead if you want lossless.")
                    .defineInRange("compression", 0.9, 0.0,1.0);
            makeJPEGs = builder
                    .comment("Enable to allow Nicephore to make lossy JPEGs of your screenshots for easier online sharing. Disable to only allow PNGs." +
                            "\r\nNote that PNGs will still be made regardless of this option.")
                    .define("makeJPEGs", true);
            builder.pop();
        }
        public static float getCompressionLevel() {
            return CLIENT.compression.get().floatValue();
        }
        public static boolean getJPEGToggle() { return CLIENT.makeJPEGs.get(); }
    }
}
