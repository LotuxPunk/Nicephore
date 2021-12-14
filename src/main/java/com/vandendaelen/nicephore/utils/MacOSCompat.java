package com.vandendaelen.nicephore.utils;

import ca.weblite.objc.Client;
import ca.weblite.objc.Proxy;
import com.vandendaelen.nicephore.Nicephore;
import net.minecraft.client.Minecraft;


//Source : https://github.com/comp500/ScreenshotToClipboard/blob/1.16-arch/common/src/main/java/link/infra/screenshotclipboard/MacOSCompat.java
public class MacOSCompat {
    public static void doCopyMacOS(String path) {
        if (!Minecraft.ON_OSX) {
            return;
        }

        Client client = Client.getInstance();
        Proxy url = client.sendProxy("NSURL", "fileURLWithPath:", path);

        Proxy image = client.sendProxy("NSImage", "alloc");
        image.send("initWithContentsOfURL:", url);

        Proxy array = client.sendProxy("NSArray", "array");
        array = array.sendProxy("arrayByAddingObject:", image);

        Proxy pasteboard = client.sendProxy("NSPasteboard", "generalPasteboard");
        pasteboard.send("clearContents");
        boolean wasSuccessful = pasteboard.sendBoolean("writeObjects:", array);
        if (!wasSuccessful) {
            Nicephore.LOGGER.error("Failed to write image to pasteboard!");
        }
    }
}
