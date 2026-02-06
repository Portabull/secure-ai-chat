package com.worktree.secure.app;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

public class ScreenshotUtil {

    public static String captureBase64() {
        try {
            Rectangle screen =
                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage img =
                    new Robot().createScreenCapture(screen);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }
}
