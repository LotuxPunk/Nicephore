package com.vandendaelen.nicephore.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class CopyImageToClipBoard implements ClipboardOwner {
    private static File lastScreenshot = null;

    public static void setLastScreenshot(File screenshot){
        lastScreenshot = screenshot;
    }
    public void copyImage(BufferedImage bi) throws IOException {
        if (Objects.equals(System.getProperty("java.awt.headless"), "false")){
            final TransferableImage trans = new TransferableImage(bi);
            final Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
            c.setContents( trans, this );
        }
        else {
            throw new IOException("Couldn't copy the screenshot");
        }
    }

    public void copyLastScreenshot() throws IOException {
        if ( lastScreenshot != null ) {
            copyImage(ImageIO.read(lastScreenshot));
        } else {
            throw new IOException("No screenshot taken");
        }
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