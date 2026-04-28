package test;

import indexer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PathScorer Tests")
class PathScorerTest {

    private PathScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new PathScorer();
    }

    @Test
    @DisplayName("Shorter paths score higher than deeper paths")
    void shorterPathScoresHigher() {
        long now = System.currentTimeMillis();
        FileRecord shallow = createRecord("/home/user/file.java", ".java", 5000, now, false);
        FileRecord deep = createRecord("/home/user/a/b/c/d/e/f/g/file.java", ".java", 5000, now, false);

        assertTrue(scorer.score(shallow) > scorer.score(deep));
    }

    @Test
    @DisplayName("High-priority extensions score higher than unknown extensions")
    void extensionPriority() {
        long now = System.currentTimeMillis();
        FileRecord java = createRecord("/home/file.java", ".java", 5000, now, false);
        FileRecord unknown = createRecord("/home/file.xyz", ".xyz", 5000, now, false);

        assertTrue(scorer.score(java) > scorer.score(unknown));
    }

    @Test
    @DisplayName("Recently modified files score higher than old files")
    void recencyBoost() {
        long now = System.currentTimeMillis();
        long oneYearAgo = now - (365L * 24 * 60 * 60 * 1000);

        FileRecord recent = createRecord("/home/file.java", ".java", 5000, now, false);
        FileRecord old = createRecord("/home/file.java", ".java", 5000, oneYearAgo, false);

        assertTrue(scorer.score(recent) > scorer.score(old));
    }

    @Test
    @DisplayName("Files in important directories get a boost")
    void directoryImportance() {
        long now = System.currentTimeMillis();
        FileRecord inSrc = createRecord("/home/user/Documents/project/src/file.java", ".java", 5000, now, false);
        FileRecord inRandom = createRecord("/home/user/randomfolder/stuff/file.java", ".java", 5000, now, false);

        assertTrue(scorer.score(inSrc) > scorer.score(inRandom));
    }

    @Test
    @DisplayName("Hidden files are penalized")
    void hiddenFilePenalty() {
        long now = System.currentTimeMillis();
        FileRecord visible = createRecord("/home/file.java", ".java", 5000, now, false);
        FileRecord hidden = createRecord("/home/file.java", ".java", 5000, now, true);

        assertTrue(scorer.score(visible) > scorer.score(hidden));
    }

    private FileRecord createRecord(String path, String extension, long size, long lastModified, boolean isHidden) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        return new FileRecord(path, name, extension, size, lastModified, lastModified,
                isHidden, true, "text/plain", "code", "", "");
    }
}