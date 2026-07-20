package org.example.service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Helper to build WhatsApp wa.me links and open them in the default browser.
 * Also opens the containing folder for the provided PDF so user can attach it quickly.
 */
public final class WhatsappService {

    private WhatsappService() {}

    public static void openWhatsappWithMessage(String phoneNumber, String message, Path pdfPath) throws IOException {
        // phoneNumber in international format without +, e.g. 919999888777
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String waUrl = "https://wa.me/" + phoneNumber + "?text=" + encoded;
        if (Desktop.isDesktopSupported()) {
            Desktop dt = Desktop.getDesktop();
            try {
                dt.browse(URI.create(waUrl));
            } catch (Exception e) {
                // ignore browse failures
            }
            if (pdfPath != null) {
                File f = pdfPath.toFile();
                File parent = f.getParentFile();
                if (parent != null && parent.exists()) {
                    try { dt.open(parent); } catch (Exception ignore) {}
                }
            }
        } else {
            throw new IOException("Desktop is not supported on this platform");
        }
    }

}
