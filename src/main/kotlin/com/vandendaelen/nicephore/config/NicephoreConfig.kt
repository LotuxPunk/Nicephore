package com.vandendaelen.nicephore.config

import com.vandendaelen.nicephore.enums.OperatingSystems
import com.vandendaelen.nicephore.enums.ScreenshotFilter
import net.neoforged.neoforge.common.ModConfigSpec

object NicephoreConfig {
    val CLIENT: Client
    val CLIENT_SPEC: ModConfigSpec

    init {
        val pair = ModConfigSpec.Builder().configure { builder -> Client(builder) }
        CLIENT_SPEC = pair.right
        CLIENT = pair.left
    }

    class Client(builder: ModConfigSpec.Builder) {
        val compression: ModConfigSpec.DoubleValue
        val makeJPEGs: ModConfigSpec.BooleanValue
        val optimisedOutput: ModConfigSpec.BooleanValue
        val showOptimisationStatus: ModConfigSpec.BooleanValue
        val screenshotToClipboard: ModConfigSpec.BooleanValue
        val screenshotCustomMessage: ModConfigSpec.BooleanValue
        val pngOptimisationLevel: ModConfigSpec.IntValue
        val screenshotFilter: ModConfigSpec.EnumValue<ScreenshotFilter>

        init {
            builder.push("Client settings")

            optimisedOutput = builder
                .comment(
                    "Enable to allow Nicephore to losslessly optimise the PNG and JPEG screenshots for smaller sized progressive files that are of identical quality to the files before optimisation.",
                    "Note: Enabling this will cause screenshots to take slightly longer to save as an optimisation step will have to be run first.",
                    "Tip: In the rare case that a screenshot PNG is corrupted, run \"oxipng --fix (filename).png\" to attempt to fix it."
                )
                .worldRestart()
                .define("optimiseScreenshots", OperatingSystems.getOS() == OperatingSystems.WINDOWS)

            showOptimisationStatus = builder
                .comment("If enabled, a message will appear above your hotbar telling you that has optimisation started and another when finished. Useful for very slow computers.")
                .define("showOptimisationStatus", true)

            screenshotToClipboard = builder
                .comment("Automatically put newly made screenshots into your clipboard")
                .define("screenshotToClipboard", true)

            screenshotCustomMessage = builder
                .comment(
                    "Display a custom message when a screenshot is made.",
                    "This message show some option to open directly the different screenshots made or the folder."
                )
                .define("screenshotCustomMessage", true)

            builder.push("GUI-specific settings")

            screenshotFilter = builder
                .comment("Only show the PNG, JPEG or JPEG/PNG on the screenshot GUI")
                .defineEnum("screenshotFilter", ScreenshotFilter.BOTH)

            builder.pop()

            builder.push("JPEG-specific settings")

            compression = builder
                .comment(
                    "JPEG compression level, the higher the number, the better the quality.",
                    "Note that 1.0 is *not* lossless as JPEG is a lossy-only format, use the PNG files instead if you want lossless."
                )
                .defineInRange("compression", 0.9, 0.0, 1.0)

            makeJPEGs = builder
                .comment(
                    "Enable to allow Nicephore to make lossy JPEGs of your screenshots for easier online sharing. Disable to only allow PNGs.",
                    "Note that PNGs will still be made regardless of this option."
                )
                .define("makeJPEGs", OperatingSystems.getOS() == OperatingSystems.WINDOWS)

            builder.pop()

            builder.push("PNG-specific settings")
            pngOptimisationLevel = builder
                .comment(
                    "If optimiseScreenshots is enabled, use the following oxipng optimisation level, with higher numbers taking longer to process but give lower file sizes.",
                    "Tip: I would avoid anything above 3 unless you have a lot of CPU cores and threads (e.g. 16c/32t+) as it starts taking significantly longer to process for vastly diminishing returns."
                )
                .defineInRange("pngOptimisationLevel", 2, 0, 5)
            builder.pop()

            builder.pop()
        }

        companion object {
            fun getCompressionLevel(): Float = NicephoreConfig.CLIENT.compression.get().toFloat()

            fun getJPEGToggle(): Boolean = NicephoreConfig.CLIENT.makeJPEGs.get()

            fun setJPEGToggle(value: Boolean) {
                NicephoreConfig.CLIENT.makeJPEGs.set(value)
            }

            fun getOptimisedOutputToggle(): Boolean = NicephoreConfig.CLIENT.optimisedOutput.get()

            fun getShouldShowOptStatus(): Boolean = NicephoreConfig.CLIENT.showOptimisationStatus.get()

            fun setShouldShowOptStatus(value: Boolean) {
                NicephoreConfig.CLIENT.showOptimisationStatus.set(value)
            }

            fun getScreenshotToClipboard(): Boolean = NicephoreConfig.CLIENT.screenshotToClipboard.get()

            fun setScreenshotToClipboard(value: Boolean) {
                NicephoreConfig.CLIENT.screenshotToClipboard.set(value)
            }

            fun getScreenshotCustomMessage(): Boolean = NicephoreConfig.CLIENT.screenshotCustomMessage.get()

            fun setScreenshotCustomMessage(value: Boolean) {
                NicephoreConfig.CLIENT.screenshotCustomMessage.set(value)
            }

            fun getPNGOptimisationLevel(): Byte = NicephoreConfig.CLIENT.pngOptimisationLevel.get().toByte()

            fun getScreenshotFilter(): ScreenshotFilter = NicephoreConfig.CLIENT.screenshotFilter.get()

            fun setScreenshotFilter(filter: ScreenshotFilter) {
                NicephoreConfig.CLIENT.screenshotFilter.set(filter)
            }
        }
    }
}
