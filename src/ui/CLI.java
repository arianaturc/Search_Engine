package ui;

import search.Formatter;
import search.ResultFormatter;
import search.SearchService;

import java.util.Scanner;

public class CLI {

    private final SearchService searchService;
    private final Formatter formatter = new ResultFormatter();

    public CLI(SearchService searchService) {
        this.searchService = searchService;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Local File Search Engine ===");
        System.out.println("Type a query to search. Type 'exit' to quit.");
        System.out.println("─".repeat(60));

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

            System.out.println(formatter.format(searchService.search(input)));
        }

        scanner.close();
    }
}