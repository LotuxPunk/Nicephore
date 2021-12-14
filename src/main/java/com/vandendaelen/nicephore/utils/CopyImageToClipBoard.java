package com.vandendaelen.nicephore.utils;

import com.mojang.blaze3d.platform.ClipboardManager;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Objects;

public class CopyImageToClipBoard implements ClipboardOwner {
    private static File lastScreenshot = null;
    private final ClipboardManager clipboardManager = new ClipboardManager();

    public static void setLastScreenshot(File screenshot){
        lastScreenshot = screenshot;
    }
    public void copyImage(BufferedImage bi, File screenshot) {
        if (Minecraft.ON_OSX) {
            MacOSCompat.doCopyMacOS(screenshot.getPath());
        }
        else {
            if (Objects.equals(System.getProperty("java.awt.headless"), "false")){
                final TransferableImage trans = new TransferableImage(bi);
                final Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                c.setContents( trans, this );
            }
        }
    }

    public void copyLastScreenshot() throws IOException {
        if ( lastScreenshot != null ) {
            copyImage(ImageIO.read(lastScreenshot), lastScreenshot);
        } else {
            throw new IOException("No screenshot taken");
        }
    }

    public static String imgToBase64String(final RenderedImage img, final String formatName) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (final OutputStream b64os = Base64.getEncoder().wrap(os)) {
            ImageIO.write(img, formatName, b64os);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        return os.toString();
    }

    public void lostOwnership( Clipboard clip, Transferable trans ) {
        System.out.println( "Lost Clipboard Ownership" );
    }

    private static class TransferableImage implements Transferable {

        final Image i;
        public TransferableImage( Image i ) {
            this.i = i;
        }

        public Object getTransferData( DataFlavor flavor ) throws UnsupportedFlavorException {
            if ( flavor.equals( DataFlavor.imageFlavor ) && i != null ) {
                return i;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] flavors = new DataFlavor[ 1 ];
            flavors[ 0 ] = DataFlavor.imageFlavor;
            return flavors;
        }

        public boolean isDataFlavorSupported( DataFlavor flavor ) {
            final DataFlavor[] flavors = getTransferDataFlavors();
            for ( DataFlavor dataFlavor : flavors ) {
                if ( flavor.equals(dataFlavor) ) {
                    return true;
                }
            }
            return false;
        }
    }
}