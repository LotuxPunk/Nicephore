package com.vandendaelen.nicephore.thread;

import com.vandendaelen.nicephore.Nicephore;
import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
import com.vandendaelen.nicephore.utils.Reference;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public final class JPEGThread extends Thread {
    private final NativeImage image;
    private final File screenshot;

    public JPEGThread(NativeImage image, File screenshot) {
        this.image = image;
        this.screenshot = screenshot;
    }

    @Override
    public void run() {
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(image.asByteArray());
            final BufferedImage png = ImageIO.read(bais);
            final File jpegFile = new File(screenshot.getParentFile(), screenshot.getName().replace("png", "jpg"));
            final BufferedImage result = new BufferedImage(
                    png.getWidth(),
                    png.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            result.createGraphics().drawImage(png, 0, 0, Color.WHITE, null);

            // only run JPEG creation-related code if "makeJPEGs" is true in the config
            if (NicephoreConfig.Client.getJPEGToggle()) {
                final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                final ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
                params.setCompressionQuality(NicephoreConfig.Client.getCompressionLevel());
                writer.setOutput(new FileImageOutputStream(jpegFile));
                writer.write(null, new IIOImage(result, null, null), params);
            }

            // only run optimisation-related code if "optimiseScreenshots" is true in the config
            if (NicephoreConfig.Client.getOptimisedOutputToggle()) {
                final boolean shouldShowOptStatus = NicephoreConfig.Client.getShouldShowOptStatus();
                if (shouldShowOptStatus) {
                    PlayerHelper.sendHotbarMessage(new TranslationTextComponent("nicephore.screenshot.optimize"));
                }

                // only run JPEG optimisation with ECT if we "makeJPEGs" is true in the config
                if (NicephoreConfig.Client.getJPEGToggle()) {

                    // attempt to optimise the JPEG screenshot using ECT
                    try {
                        final File ect = new File(String.format("mods%snicephore%s", File.separator, File.separator) + Reference.File.ECT);
                        // ECT is lightning fast for small JPEG files so we might as well use optimisation level 9
                        final Process p = Runtime.getRuntime().exec(MessageFormat.format(Reference.Command.ECT, ect, jpegFile));
                        p.waitFor();
                    } catch (IOException | InterruptedException e) {
                        Nicephore.LOGGER.warn("Unable to optimise screenshot JPEG with ECT. Is it missing from the mods folder?");
                        Nicephore.LOGGER.warn(e.getMessage());
                    }
                }

                // attempt to optimise the PNG screenshot using Oxipng
                try {
                    final File oxipng = new File(String.format("mods%snicephore%s", File.separator, File.separator) + Reference.File.OXIPNG);
                    final File pngFile = new File(screenshot.getParentFile(), screenshot.getName());
                    final Process p = Runtime.getRuntime().exec(MessageFormat.format(Reference.Command.OXIPNG, oxipng, NicephoreConfig.Client.getPNGOptimisationLevel(), pngFile));
                    p.waitFor();
                } catch (IOException | InterruptedException e) {
                    Nicephore.LOGGER.warn("Unable to optimise screenshot PNG with Oxipng. Is it missing from the mods folder?");
                    Nicephore.LOGGER.warn(e.getMessage());
                }

                if (shouldShowOptStatus) {
                    PlayerHelper.sendHotbarMessage(new TranslationTextComponent("nicephore.screenshot.optimizeFinished"));
                }
            }

            CopyImageToClipBoard.getInstance().setLastScreenshot(screenshot);

            if (NicephoreConfig.Client.getScreenshotCustomMessage()){

                if (NicephoreConfig.Client.getScreenshotToClipboard()){
                    if (CopyImageToClipBoard.getInstance().copyLastScreenshot()){
                        PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.clipboard.success").withStyle(TextFormatting.GREEN));
                    }
                    else {
                        PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.clipboard.error").withStyle(TextFormatting.RED));
                    }
                }

                final ITextComponent pngComponent = (new TranslationTextComponent("nicephore.screenshot.png")).withStyle(TextFormatting.UNDERLINE).withStyle((style)
                        -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshot.getAbsolutePath())));

                final ITextComponent jpgComponent = (new TranslationTextComponent("nicephore.screenshot.jpg")).withStyle(TextFormatting.UNDERLINE).withStyle((style)
                        -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, jpegFile.getAbsolutePath())));

                final ITextComponent folderComponent = (new TranslationTextComponent("nicephore.screenshot.folder")).withStyle(TextFormatting.UNDERLINE).withStyle((style)
                        -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshot.getParent())));

                PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.success", screenshot.getName().replace(".png", "")));

                if (NicephoreConfig.Client.getJPEGToggle()) {
                    PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.options", pngComponent, jpgComponent, folderComponent));
                } else {
                    PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.reducedOptions", pngComponent, folderComponent));
                }
            }

        } catch (IOException e) {
            Nicephore.LOGGER.error(e.getMessage());
            PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.error"));
        }
    }

}