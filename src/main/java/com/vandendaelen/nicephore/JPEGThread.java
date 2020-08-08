package com.vandendaelen.nicephore;

import com.vandendaelen.nicephore.config.NicephoreConfig;
import com.vandendaelen.nicephore.utils.CopyImageToClipBoard;
import com.vandendaelen.nicephore.utils.PlayerHelper;
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

public class JPEGThread extends Thread {
    private NativeImage image;
    private File screenshot;

    public JPEGThread(NativeImage image, File screenshot) {
        this.image = image;
        this.screenshot = screenshot;
    }

    @Override
    public void run() {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(image.getBytes());
            BufferedImage png = ImageIO.read(bais);
            File jpeg = new File(screenshot.getParentFile(), screenshot.getName().replace("png", "jpg"));
            BufferedImage result = new BufferedImage(
                    png.getWidth(),
                    png.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            result.createGraphics().drawImage(png, 0, 0, Color.WHITE, null);

            final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(NicephoreConfig.Client.getCompressionLevel());
            writer.setOutput(new FileImageOutputStream(jpeg));
            writer.write(null, new IIOImage(result, null, null), params);

            CopyImageToClipBoard.setLastScreenshot(screenshot);

            ITextComponent pngComponent = (new TranslationTextComponent("nicephore.screenshot.png")).mergeStyle(TextFormatting.UNDERLINE).modifyStyle((style) -> {
                return style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshot.getAbsolutePath()));
            });

            ITextComponent jpgComponent = (new TranslationTextComponent("nicephore.screenshot.jpg")).mergeStyle(TextFormatting.UNDERLINE).modifyStyle((style) -> {
                return style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, jpeg.getAbsolutePath()));
            });

            ITextComponent folderComponent = (new TranslationTextComponent("nicephore.screenshot.folder")).mergeStyle(TextFormatting.UNDERLINE).modifyStyle((style) -> {
                return style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, screenshot.getParent()));
            });

            PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.success", screenshot.getName().replace(".png", "")));
            PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.options", pngComponent, jpgComponent, folderComponent));
        } catch (IOException e) {
            Nicephore.LOGGER.debug(e.getMessage());
            PlayerHelper.sendMessage(new TranslationTextComponent("nicephore.screenshot.error"));
        }
    }
}
