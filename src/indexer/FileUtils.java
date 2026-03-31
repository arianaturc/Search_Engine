package indexer;

public class FileUtils {

    private FileUtils() {}

    public static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot != -1) ? fileName.substring(dot) : "";
    }
}