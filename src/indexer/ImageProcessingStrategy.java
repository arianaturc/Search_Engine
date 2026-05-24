package indexer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


public class ImageProcessingStrategy implements FileProcessingStrategy {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );

    private static final int MAX_SAMPLE_PIXELS = 10000;

    @Override
    public boolean supports(String extension) {
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    @Override
    public ProcessingResult process(Path file, String extension) {
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                System.err.println("Could not read image: " + file);
                return new ProcessingResult("", "", "");
            }

            String dominantColor = extractDominantColor(image);
            int width = image.getWidth();
            int height = image.getHeight();

            String preview = String.format("Image: %dx%d, Dominant color: %s", width, height, dominantColor);

            return new ProcessingResult("", preview, dominantColor);

        } catch (IOException e) {
            System.err.println("Error processing image: " + file + " (" + e.getMessage() + ")");
            return new ProcessingResult("", "", "");
        }
    }


    private String extractDominantColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int totalPixels = width * height;

        int step = Math.max(1, (int) Math.sqrt((double) totalPixels / MAX_SAMPLE_PIXELS));

        long totalR = 0, totalG = 0, totalB = 0;
        int sampleCount = 0;

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                int rgb = image.getRGB(x, y);

                int alpha = (rgb >> 24) & 0xFF;
                if (alpha < 128) continue;

                totalR += (rgb >> 16) & 0xFF;
                totalG += (rgb >> 8) & 0xFF;
                totalB += rgb & 0xFF;
                sampleCount++;
            }
        }

        if (sampleCount == 0)
            return "unknown";

        int avgR = (int) (totalR / sampleCount);
        int avgG = (int) (totalG / sampleCount);
        int avgB = (int) (totalB / sampleCount);

        return mapToColorName(avgR, avgG, avgB);
    }

    private String mapToColorName(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        float hue = hsb[0] * 360;
        float saturation = hsb[1];
        float brightness = hsb[2];

        if (brightness < 0.15)
            return "black";
        if (brightness > 0.9 && saturation < 0.1)
            return "white";
        if (saturation < 0.15)
            return "gray";

        if (hue < 15)  return "red";
        if (hue < 45)  return "orange";
        if (hue < 70)  return "yellow";
        if (hue < 160) return "green";
        if (hue < 200) return "cyan";
        if (hue < 260) return "blue";
        if (hue < 290) return "purple";
        if (hue < 340) return "pink";
        return "red";
    }
}