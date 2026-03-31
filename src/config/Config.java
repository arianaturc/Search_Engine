package config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    private String rootDirectory = System.getProperty("user.home") + "/Documents";
    private int maxResults = 50;
    private String reportFormat = "text";
    private Set<String> ignoredDirs = Set.of(
            "node_modules", "__MACOSX", ".git", ".idea",
            "out", "build", "target", "venv", ".venv",
            "site-packages", "pkgs", "yoloenv", "downward"
    );
    private List<String> ignoredExtensions = List.of(".exe", ".dll", ".class");

    public String getRootDirectory()    {
        return rootDirectory;
    }
    public int getMaxResults() {
        return maxResults;
    }
    public String getReportFormat() {
        return reportFormat;
    }
    public Set<String> getIgnoredDirs() {
        return ignoredDirs;
    }
    public List<String> getIgnoredExtensions() {
        return ignoredExtensions;
    }

    public void setRootDirectory(String v) {
        this.rootDirectory = v;
    }
    public void setMaxResults(int v) {
        this.maxResults = v;
    }
    public void setReportFormat(String v) {
        this.reportFormat = v;
    }
    public void setIgnoredDirs(Set<String> v) {
        this.ignoredDirs = v;
    }
    public void setIgnoredExtensions(List<String> v) {
        this.ignoredExtensions = v;
    }

    public void setIgnoredExtensionsFromString(String csv) {
        this.ignoredExtensions = Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList();
    }

    public void setIgnoredDirsFromString(String csv) {
        this.ignoredDirs = new HashSet<>(Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .toList());
    }
}