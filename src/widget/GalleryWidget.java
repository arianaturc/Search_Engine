package widget;

import search.SearchResult;

import java.util.List;
import java.util.Set;

public class GalleryWidget implements Widget {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );

    private static final double ACTIVATION_THRESHOLD = 0.4;
    private static final int MIN_IMAGES = 2;

    @Override
    public String getName() {
        return "View as Gallery";
    }

    @Override
    public String getDescription() {
        return "Display image results in a gallery view";
    }

    @Override
    public boolean shouldActivate(List<SearchResult> results, String query) {
        if (results.isEmpty()) return false;

        long imageCount = countImages(results);

        if (query != null) {
            String lowerQuery = query.toLowerCase();
            if (lowerQuery.contains("color:") || lowerQuery.contains("tag:image")) {
                return imageCount >= 1;
            }
        }

        double ratio = (double) imageCount / results.size();
        return imageCount >= MIN_IMAGES && ratio >= ACTIVATION_THRESHOLD;
    }

    @Override
    public String execute(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║         IMAGE GALLERY                ║\n");
        sb.append("╚══════════════════════════════════════╝\n");

        int count = 0;
        for (SearchResult r : results) {
            if (IMAGE_EXTENSIONS.contains(r.extension().toLowerCase())) {
                count++;
                sb.append(String.format("  [%d] %s\n", count, r.name()));
                sb.append(String.format("       Path: %s\n", r.path()));
                if (r.preview() != null && !r.preview().isBlank()) {
                    sb.append(String.format("       %s\n", r.preview()));
                }
                sb.append("\n");
            }
        }
        sb.append("Total images: ").append(count).append("\n");

        return sb.toString();
    }

    private long countImages(List<SearchResult> results) {
        return results.stream()
                .filter(r -> IMAGE_EXTENSIONS.contains(r.extension().toLowerCase()))
                .count();
    }
}