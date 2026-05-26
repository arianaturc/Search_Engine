# Changelog

## [v3.0.0] - 2026-05-26

### Added
- Multimodal search: image processor extracts dominant color, supports `color:red` queries
- FileProcessingStrategy interface (Strategy Pattern) for text vs image processing
- TextProcessingStrategy and ImageProcessingStrategy implementations
- Context-aware widgets: GalleryWidget, LogAnalyzerWidget, CodeAnalyzerWidget
- WidgetFactory (Factory Pattern) for widget activation based on search results
- Query Pre-Processor Pipeline (Decorator Pattern): SanitizationDecorator, SynonymDecorator, LogicDecorator
- Producer-Consumer architecture for indexing with multiple reader threads and single writer thread
- Thread-safe IndexingReport using AtomicInteger
- Pre-commit hook for .class file and large file checks
- Git tags for version tracking

### Changed
- Extractor refactored to use Strategy Pattern for file type handling
- IndexingService rewritten with BlockingQueue and ExecutorService
- FileRecord extended with dominantColor field
- Database schema updated with dominant_color column
- QueryProcessor supports color: qualifier
- SearchService integrates decorator pipeline and widget factory

## [v2.0.0] - 2026-04-28

### Added
- Query parser with qualifiers: path:, content:, ext:, tag:
- PathScorer for index-time file scoring (path depth, extension, recency, size, directory importance)
- Swappable ranking strategies (Strategy Pattern): Relevance, Alphabetical, Date, Size, History Boosted
- Search history with Observer Pattern: query suggestions and ranking boosts
- Search history persistence via Java serialization
- Content Snippet Highlighting with position-based scoring
- SnippetExtractor with whole-word and multi-term match prioritization
- CLI commands: :rank, :suggest, :history

### Changed
- SearchResult extended with pathScore, positionScore, snippet, content fields
- SearchRepository fetches content for snippet extraction
- ResultFormatter shows highlighted snippets instead of static previews
- GUI updated with ranking dropdown and suggestion panel
- Database schema updated with path_score column

## [v1.0.0] - 2026-03-31

### Added
- Recursive directory crawling with configurable ignore rules
- File metadata extraction (size, timestamps, MIME type, tags, hidden/readable flags)
- Charset fallback chain (UTF-8, Windows-1250, Windows-1252, ISO-8859-2)
- SQLite database with FTS5 full-text search on filename and content
- Incremental indexing (skip unchanged files, remove deleted files)
- File preview generation (first 3 non-blank lines)
- Indexing report generation (text and JSON formats)
- Edge case handling: symlink loops, access denied, binary file detection, DB reconnection
- Runtime configuration: root directory, ignored extensions, max results, report format
- CLI interface with search loop
- GUI interface (Swing) with live-as-you-type search and background re-indexing
- WAL journal mode and busy timeout for concurrent database access