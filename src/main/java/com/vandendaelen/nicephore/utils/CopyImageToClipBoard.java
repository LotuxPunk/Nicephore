package com.vandendaelen.nicephore.utils;

import com.vandendaelen.nicephore.enums.OperatingSystems;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;

public class CopyImageToClipBoard implements ClipboardOwner {
    private static File lastScreenshot = null;
    private static CopyImageToClipBoard INSTANCE;

    public static CopyImageToClipBoard getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CopyImageToClipBoard();
        }
        return INSTANCE;
    }

    public void setLastScreenshot(File screenshot){
        lastScreenshot = screenshot;
    }

    public boolean copyImage(File screenshot) {
        if (OperatingSystems.getOS().getManager() != null) {
            OperatingSystems.getOS().getManager().clipboardImage(screenshot);
            return true;
        }
        return false;
    }

    public boolean copyLastScreenshot() {
        if (lastScreenshot != null) {
            return copyImage(lastScreenshot);
        }
        return false;
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