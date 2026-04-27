package search;

import java.util.ArrayList;
import java.util.List;

public class SnippetExtractor {

    private static final int CONTEXT_LINES = 1;

    public record SnippetResult(
            String highlightedSnippet,
            double positionScore
    ) {}


    public SnippetResult extract(String content, String rawQuery) {
        if (content == null || content.isBlank() || rawQuery == null || rawQuery.isBlank()) {
            return new SnippetResult("", 0.0);
        }

        List<String> terms = extractSearchTerms(rawQuery);
        if (terms.isEmpty()) {
            return new SnippetResult("", 0.0);
        }

        String[] lines = content.split("\n");
        List<Integer> matchingLineIndices = findMatchingLines(lines, terms);

        if (matchingLineIndices.isEmpty()) {
            return new SnippetResult("", 0.0);
        }


        int earliestMatch = matchingLineIndices.stream()
                .min(Integer::compareTo)
                .orElse(matchingLineIndices.get(0));
        double totalScore = computePositionScore(earliestMatch, lines.length);

        String snippet = buildSnippets(lines, matchingLineIndices, terms);

        return new SnippetResult(snippet, totalScore);
    }


    private List<String> extractSearchTerms(String rawQuery) {
        List<String> terms = new ArrayList<>();
        String[] tokens = rawQuery.strip().split("\\s+");

        for (String token : tokens) {
            if (token.contains(":")) {
                int colonIndex = token.indexOf(':');
                String qualifier = token.substring(0, colonIndex).toLowerCase();
                String value = (colonIndex < token.length() - 1)
                        ? token.substring(colonIndex + 1) : "";

                if (qualifier.equals("content") && !value.isBlank()) {
                    String cleaned = value.replaceAll("[.:\\-*^()\"{}\\[\\]]", "").strip();
                    if (!cleaned.isBlank() && cleaned.length() >= 2) {
                        terms.add(cleaned.toLowerCase());
                    }
                }
            } else {
                String cleaned = token.replaceAll("[.:\\-*^()\"{}\\[\\]]", "").strip();
                if (!cleaned.isBlank() && cleaned.length() >= 2) {
                    terms.add(cleaned.toLowerCase());
                }
            }
        }

        return terms;
    }


    private List<Integer> findMatchingLines(String[] lines, List<String> terms) {
        List<Integer> multiTermMatches = new ArrayList<>();
        List<Integer> wholeWordMatches = new ArrayList<>();
        List<Integer> startOfWordMatches = new ArrayList<>();
        List<Integer> substringMatches = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String lineLower = lines[i].toLowerCase().strip();

            boolean anyMatch = false;
            for (String term : terms) {
                if (lineLower.contains(term)) {
                    anyMatch = true;
                    break;
                }
            }
            if (!anyMatch) continue;

            if (terms.size() > 1) {
                boolean allMatch = true;
                for (String term : terms) {
                    if (!lineLower.contains(term)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    multiTermMatches.add(i);
                    continue;
                }
            }

            boolean isWholeWord = false;
            boolean isStartOfWord = false;

            for (String term : terms) {
                if (!lineLower.contains(term)) continue;
                if (isWholeWordMatch(lineLower, term)) {
                    isWholeWord = true;
                    break;
                }
                if (isStartOfWordMatch(lineLower, term)) {
                    isStartOfWord = true;
                }
            }

            if (isWholeWord) {
                wholeWordMatches.add(i);
            } else if (isStartOfWord) {
                startOfWordMatches.add(i);
            } else {
                substringMatches.add(i);
            }
        }


        List<Integer> prioritized = new ArrayList<>();
        prioritized.addAll(multiTermMatches);
        prioritized.addAll(wholeWordMatches);
        prioritized.addAll(startOfWordMatches);
        prioritized.addAll(substringMatches);
        return prioritized;
    }


    private boolean isWholeWordMatch(String line, String term) {
        int index = 0;
        while ((index = line.indexOf(term, index)) != -1) {
            boolean startOk = (index == 0) || !Character.isLetterOrDigit(line.charAt(index - 1));
            int endPos = index + term.length();
            boolean endOk = (endPos >= line.length()) || !Character.isLetterOrDigit(line.charAt(endPos));
            if (startOk && endOk) return true;
            index += term.length();
        }
        return false;
    }


    private boolean isStartOfWordMatch(String line, String term) {
        int index = 0;
        while ((index = line.indexOf(term, index)) != -1) {
            boolean startOk = (index == 0) || !Character.isLetterOrDigit(line.charAt(index - 1));
            if (startOk) return true;
            index += term.length();
        }
        return false;
    }


    private double computePositionScore(int firstMatchLine, int totalLines) {
        if (totalLines <= 1) return 30.0;
        double ratio = 1.0 - ((double) firstMatchLine / totalLines);
        return 30.0 * ratio;
    }


    private String buildSnippets(String[] lines, List<Integer> matchingIndices, List<String> terms) {
        if (matchingIndices.isEmpty()) return "";

        int earliestMatch = matchingIndices.stream()
                .min(Integer::compareTo)
                .orElse(matchingIndices.get(0));

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, earliestMatch - CONTEXT_LINES);
        int end = Math.min(lines.length - 1, earliestMatch + CONTEXT_LINES);

        for (int i = start; i <= end; i++) {
            String lineNum = String.format("%4d", i + 1);
            String line = lines[i];

            boolean isMatchLine = containsAnyTerm(lines[i].toLowerCase(), terms);
            if (isMatchLine) {
                line = highlightTerms(line, terms);
                sb.append("   >> ").append(lineNum).append(" | ").append(line).append("\n");
            } else {
                sb.append("      ").append(lineNum).append(" | ").append(line).append("\n");
            }
        }

        return sb.toString();
    }

    private boolean containsAnyTerm(String lineLower, List<String> terms) {
        for (String term : terms) {
            if (lineLower.contains(term)) return true;
        }
        return false;
    }

    private String highlightTerms(String line, List<String> terms) {
        String result = line;
        for (String term : terms) {
            StringBuilder highlighted = new StringBuilder();
            String lowerResult = result.toLowerCase();
            int searchFrom = 0;

            while (true) {
                int index = lowerResult.indexOf(term, searchFrom);
                if (index == -1) {
                    highlighted.append(result, searchFrom, result.length());
                    break;
                }
                highlighted.append(result, searchFrom, index);
                highlighted.append(" >>")
                        .append(result, index, index + term.length())
                        .append("<< ");
                searchFrom = index + term.length();
            }

            result = highlighted.toString();
        }
        return result;
    }
}