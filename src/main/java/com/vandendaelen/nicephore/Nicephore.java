package com.vandendaelen.nicephore;

import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

// The value here should match an entry in the META-INF/mods.toml file
@Mod("nicephore")
public class Nicephore {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public Nicephore() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onScreenshot(ScreenshotEvent event) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(event.getImage().getBytes());
            BufferedImage png = ImageIO.read(bais);
            File jpeg = new File(event.getScreenshotFile().getParentFile(), event.getScreenshotFile().getName().replace("png", "jpg"));
            BufferedImage result = new BufferedImage(
                    png.getWidth(),
                    png.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            result.createGraphics().drawImage(png, 0, 0, Color.WHITE, null);

            final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            writer.setOutput(new FileImageOutputStream(jpeg));
            writer.write(null, new IIOImage(result, null, null), params);
//            ImageIO.write(result, "jpg", jpeg);
            //event.setScreenshotFile(jpeg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
