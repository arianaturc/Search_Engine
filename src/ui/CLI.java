package ui;

import search.*;

import java.util.List;
import java.util.Scanner;

public class CLI {

    private final SearchService searchService;
    private final Formatter formatter = new ResultFormatter();
    private final SearchHistory searchHistory;

    private final RankingStrategy[] strategies;
    private int currentStrategyIndex = 0;

    public CLI(SearchService searchService, SearchHistory searchHistory) {
        this.searchService = searchService;
        this.searchHistory = searchHistory;

        this.strategies = new RankingStrategy[]{
                new RelevanceRanking(),
                new AlphabeticalRanking(),
                new DateRanking(),
                new SizeRanking(),
                new HistoryBoostedRanking(searchHistory)
        };
        searchService.setRankingStrategy(strategies[currentStrategyIndex]);
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Local File Search Engine ===");
        System.out.println("Type a query to search. Type 'exit' to quit.");
        System.out.println("  :rank      — cycle ranking strategy");
        System.out.println("  :suggest   — show query suggestions");
        System.out.println("  :history   — show recent searches");
        System.out.println("  exit       — quit");
        System.out.println("─".repeat(60));
        System.out.println("Ranking: " + strategies[currentStrategyIndex].getName());

        while (true) {
            System.out.print("\nSearch: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting...");
                break;
            }

            if (input.isBlank()) {
                System.out.println("Please enter a search term.");
                continue;
            }

            if (input.equalsIgnoreCase(":rank")) {
                cycleRankingStrategy();
                continue;
            }

            if (input.toLowerCase().startsWith(":suggest")) {
                String prefix = input.length() > 8 ? input.substring(8).strip() : "";
                showSuggestions(prefix);
                continue;
            }

            if (input.equalsIgnoreCase(":history")) {
                showHistory();
                continue;
            }


            List<SearchResult> results = searchService.search(input);
            System.out.println(formatter.format(results));
        }

        scanner.close();
    }

    private void cycleRankingStrategy() {
        currentStrategyIndex = (currentStrategyIndex + 1) % strategies.length;
        searchService.setRankingStrategy(strategies[currentStrategyIndex]);
        System.out.println("Ranking changed to: " + strategies[currentStrategyIndex].getName());
    }

    private void showSuggestions(String prefix) {
        List<String> suggestions = searchHistory.suggest(prefix);
        if (suggestions.isEmpty()) {
            System.out.println("No suggestions available.");
        } else {
            System.out.println("Suggestions:");
            for (int i = 0; i < suggestions.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + suggestions.get(i));
            }
        }
    }

    private void showHistory() {
        List<String> recent = searchHistory.getRecentQueries(10);
        if (recent.isEmpty()) {
            System.out.println("No search history yet.");
        } else {
            System.out.println("Recent searches:");
            for (int i = 0; i < recent.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + recent.get(i));
            }
        }
    }
}