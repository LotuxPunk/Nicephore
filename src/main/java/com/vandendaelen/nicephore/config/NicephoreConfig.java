package com.vandendaelen.nicephore.config;

import com.vandendaelen.nicephore.enums.OperatingSystems;
import com.vandendaelen.nicephore.enums.ScreenshotFilter;
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
        public final ForgeConfigSpec.BooleanValue makeJPEGs, optimisedOutput, showOptimisationStatus, screenshotToClipboard, screenshotCustomMessage;
        public final ForgeConfigSpec.IntValue pngOptimisationLevel;
        public final ForgeConfigSpec.EnumValue<ScreenshotFilter> screenshotFilter;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("Client settings");

            optimisedOutput = builder
                    .comment("Enable to allow Nicephore to losslessly optimise the PNG and JPEG screenshots for smaller sized progressive files that are of identical quality to the files before optimisation.", "Note: Enabling this will cause screenshots to take slightly longer to save as an optimisation step will have to be run first.", "Tip: In the rare case that a screenshot PNG is corrupted, run \"oxipng --fix (filename).png\" to attempt to fix it.")
                    .worldRestart()
                    .define("optimiseScreenshots", OperatingSystems.getOS().equals(OperatingSystems.WINDOWS));

            showOptimisationStatus = builder
                    .comment("If enabled, a message will appear above your hotbar telling you that has optimisation started and another when finished. Useful for very slow computers.")
                    .define("showOptimisationStatus", true);

            screenshotToClipboard = builder
                    .comment("Automatically put newly made screenshots into your clipboard")
                    .define("screenshotToClipboard", true);

            screenshotCustomMessage = builder
                    .comment("Display a custom message when a screenshot is made.", "This message show some option to open directly the different screenshots made or the folder.")
                    .define("screenshotCustomMessage", true);

            builder.push("GUI-specific settings");

            screenshotFilter = builder
                    .comment("Only show the PNG, JPEG or JPEG/PNG on the screenshot GUI")
                    .defineEnum("screenshotFilter", ScreenshotFilter.BOTH);

            builder.pop();

            builder.push("JPEG-specific settings");

            compression = builder
                    .comment("JPEG compression level, the higher the number, the better the quality.", "Note that 1.0 is *not* lossless as JPEG is a lossy-only format, use the PNG files instead if you want lossless.")
                    .defineInRange("compression", 0.9, 0.0, 1.0);

            makeJPEGs = builder
                    .comment("Enable to allow Nicephore to make lossy JPEGs of your screenshots for easier online sharing. Disable to only allow PNGs.", "Note that PNGs will still be made regardless of this option.")
                    .define("makeJPEGs", OperatingSystems.getOS().equals(OperatingSystems.WINDOWS));

            builder.pop();

            builder.push("PNG-specific settings");
            pngOptimisationLevel = builder
                    .comment("If optimiseScreenshots is enabled, use the following oxipng optimisation level, with higher numbers taking longer to process but give lower file sizes.", "Tip: I would avoid anything above 3 unless you have a lot of CPU cores and threads (e.g. 16c/32t+) as it starts taking significantly longer to process for vastly diminishing returns.")
                    .defineInRange("pngOptimisationLevel", 2, 0, 5);
            builder.pop();

            builder.pop();
        }

        public static float getCompressionLevel() {
            return CLIENT.compression.get().floatValue();
        }

        public static boolean getJPEGToggle() {
            return CLIENT.makeJPEGs.get();
        }

        public static void setJPEGToggle(boolean value) {
            CLIENT.makeJPEGs.set(value);
        }

        public static boolean getOptimisedOutputToggle() {
            return CLIENT.optimisedOutput.get();
        }

        public static boolean getShouldShowOptStatus() {
            return CLIENT.showOptimisationStatus.get();
        }

        public static void setShouldShowOptStatus(boolean value) {
            CLIENT.showOptimisationStatus.set(value);
        }

        public static boolean getScreenshotToClipboard() {
            return CLIENT.screenshotToClipboard.get();
        }

        public static void setScreenshotToClipboard(boolean value) {
            CLIENT.screenshotToClipboard.set(value);
        }

        public static boolean getScreenshotCustomMessage() {
            return CLIENT.screenshotCustomMessage.get();
        }

        public static void setScreenshotCustomMessage(boolean value) {
            CLIENT.screenshotCustomMessage.set(value);
        }

        public static byte getPNGOptimisationLevel() {
            return CLIENT.pngOptimisationLevel.get().byteValue();
        }

        public static ScreenshotFilter getScreenshotFilter() {
            return CLIENT.screenshotFilter.get();
        }

        public static void setScreenshotFilter(ScreenshotFilter filter) {
            CLIENT.screenshotFilter.set(filter);
        }
    }
}
