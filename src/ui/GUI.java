package ui;

import config.Config;
import database.FileRepository;
import indexer.IndexingService;
import search.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GUI {

    private final SearchService   searchService;
    private final Config          config;
    private final FileRepository  fileRepository;
    private final IndexingService indexingService;
    private final SearchHistory searchHistory;
    private final Formatter       formatter = new ResultFormatter();

    private final RankingStrategy[] strategies;

    public GUI(SearchService searchService, Config config,
               FileRepository fileRepository, IndexingService indexingService,
               SearchHistory searchHistory) {
        this.searchService   = searchService;
        this.config          = config;
        this.fileRepository  = fileRepository;
        this.indexingService = indexingService;
        this.searchHistory   = searchHistory;

        this.strategies = new RankingStrategy[]{
                new RelevanceRanking(),
                new AlphabeticalRanking(),
                new DateRanking(),
                new SizeRanking(),
                new HistoryBoostedRanking(searchHistory)
        };
    }

    public void start() {
        JFrame frame = new JFrame("Local File Search Engine");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        JTextField rootField       = new JTextField(config.getRootDirectory());
        JTextField ignoreExtField  = new JTextField(String.join(", ", config.getIgnoredExtensions()));
        JTextField maxResultsField = new JTextField(String.valueOf(config.getMaxResults()));
        JComboBox<String> reportFormatBox = new JComboBox<>(new String[]{"text", "json"});
        reportFormatBox.setSelectedItem(config.getReportFormat());

        String[] strategyNames = new String[strategies.length];
        for (int i = 0; i < strategies.length; i++) {
            strategyNames[i] = strategies[i].getName();
        }
        JComboBox<String> rankingBox = new JComboBox<>(strategyNames);
        rankingBox.setSelectedIndex(0);

        configPanel.add(createFieldRow("Root Directory:",      rootField));
        configPanel.add(Box.createVerticalStrut(5));
        configPanel.add(createFieldRow("Ignored Extensions:",  ignoreExtField));
        configPanel.add(Box.createVerticalStrut(5));
        configPanel.add(createFieldRow("Max Results:",         maxResultsField));
        configPanel.add(Box.createVerticalStrut(5));
        configPanel.add(createFieldRow("Report Format:",       reportFormatBox));
        configPanel.add(Box.createVerticalStrut(5));
        configPanel.add(createFieldRow("Ranking Strategy:",    rankingBox));
        configPanel.add(Box.createVerticalStrut(8));

        JButton reindexButton = new JButton("Re-Index");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(reindexButton);
        configPanel.add(buttonPanel);

        JTextField searchBar = new JTextField();
        searchBar.setFont(new Font("Arial", Font.PLAIN, 16));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        DefaultListModel<String> suggestionModel = new DefaultListModel<>();
        JList<String> suggestionList = new JList<>(suggestionModel);
        suggestionList.setFont(new Font("Arial", Font.PLAIN, 14));
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane suggestionScroll = new JScrollPane(suggestionList);
        suggestionScroll.setPreferredSize(new Dimension(0, 120));
        suggestionScroll.setVisible(false);

        suggestionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && suggestionList.getSelectedValue() != null) {
                searchBar.setText(suggestionList.getSelectedValue());
                suggestionScroll.setVisible(false);
                searchBar.requestFocusInWindow();
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchBar, BorderLayout.CENTER);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        topPanel.add(configPanel);
        topPanel.add(searchPanel);

        JTextArea resultsArea = new JTextArea();
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultsArea.setEditable(false);
        resultsArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JLabel statusBar = new JLabel("Ready.");
        statusBar.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        statusBar.setForeground(Color.GRAY);

        rankingBox.addActionListener(e -> {
            int selectedIndex = rankingBox.getSelectedIndex();
            searchService.setRankingStrategy(strategies[selectedIndex]);
            statusBar.setText("Ranking: " + strategies[selectedIndex].getName());

            String input = searchBar.getText();
            if (!input.isBlank()) {
                List<SearchResult> results = searchService.search(input);
                resultsArea.setText(formatter.format(results));
                resultsArea.setCaretPosition(0);
                statusBar.setText("Found " + results.size() + " result(s) | Ranking: "
                        + strategies[selectedIndex].getName());
            }
        });

        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { runSearch(); }
            @Override public void removeUpdate(DocumentEvent e)  { runSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { runSearch(); }

            private void runSearch() {
                String input = searchBar.getText();
                if (input.isBlank()) {
                    resultsArea.setText("");
                    statusBar.setText("Ready. | Ranking: " + searchService.getRankingStrategy().getName());
                    suggestionScroll.setVisible(false);
                    return;
                }

                List<String> suggestions = searchHistory.suggest(input);
                suggestionModel.clear();
                if (!suggestions.isEmpty()) {
                    for (String s : suggestions) {
                        suggestionModel.addElement(s);
                    }
                    suggestionScroll.setVisible(true);
                } else {
                    suggestionScroll.setVisible(false);
                }

                List<SearchResult> results = searchService.search(input);
                resultsArea.setText(formatter.format(results));
                resultsArea.setCaretPosition(0);
                statusBar.setText("Found " + results.size() + " result(s) for: " + input
                        + " | Ranking: " + searchService.getRankingStrategy().getName());
            }
        });

        searchBar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    suggestionScroll.setVisible(false);
                }
            }
        });

        reindexButton.addActionListener(e -> {
            String newRoot = rootField.getText().strip();
            if (newRoot.isEmpty() || !Files.isDirectory(Path.of(newRoot))) {
                JOptionPane.showMessageDialog(frame,
                        "Invalid root directory: " + newRoot,
                        "Configuration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            config.setRootDirectory(newRoot);
            config.setIgnoredExtensionsFromString(ignoreExtField.getText());

            try {
                config.setMaxResults(Integer.parseInt(maxResultsField.getText().strip()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Invalid max results value. Using previous value.",
                        "Configuration Error", JOptionPane.WARNING_MESSAGE);
            }

            config.setReportFormat((String) reportFormatBox.getSelectedItem());

            reindexButton.setEnabled(false);
            statusBar.setText("Re-indexing...");
            resultsArea.setText("");

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return indexingService.runIndex();
                }

                @Override
                protected void done() {
                    try {
                        resultsArea.setText(get());
                        statusBar.setText("Re-indexing complete. | Ranking: "
                                + searchService.getRankingStrategy().getName());
                    } catch (Exception ex) {
                        resultsArea.setText("Re-indexing failed: " + ex.getMessage());
                        statusBar.setText("Re-indexing failed.");
                    }
                    reindexButton.setEnabled(true);
                }
            }.execute();
        });

        frame.add(topPanel,   BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(statusBar,  BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createFieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        JLabel jLabel = new JLabel(label);
        jLabel.setPreferredSize(new Dimension(140, 25));
        row.add(jLabel, BorderLayout.WEST);
        row.add(field,  BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return row;
    }
}