# Local File Search Engine

A local file search system built in Java that indexes files on the device and enables fast, full-text search across filenames and file contents. The engine crawls directories recursively, extracts metadata and text content, and provides both a command-line interface and a graphical interface.

---

## Table of Contents

- [Features](#features)
- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [Component Breakdown](#component-breakdown)
  - [config](#config)
  - [database](#database)
  - [indexer](#indexer)
  - [search](#search)
  - [ui](#ui)
- [How It Works](#how-it-works)
  - [Indexing Pipeline](#indexing-pipeline)
  - [Search Pipeline](#search-pipeline)
- [Technologies Used](#technologies-used)

---

## Features

- **Recursive directory crawling** with configurable ignore rules for extensions and directories.
- **Full-text search** across both filenames and file contents using SQLite FTS5.
- **Incremental indexing** — only new or modified files are re-indexed; deleted files are automatically removed.
- **File preview generation** — the first 3 non-blank lines of text files are stored for quick display.
- **Metadata extraction** — size, timestamps, MIME type, tags, hidden/readable flags.
- **Edge case handling** — symlink loop detection, access-denied recovery, binary file skipping, database reconnection.
- **Runtime configuration** — root directory, ignored extensions, max results, and report format can all be changed without restarting.
- **Indexing report** — summary of indexed, unchanged, skipped, removed, and failed files, in text or JSON format.
- **Dual interface** — CLI for terminal use, GUI (Swing) with live search and background re-indexing.

---

## Project Structure

```
Search-Engine/
├── data/                          # SQLite database storage
├── out/                           # Compiled output
└── src/
    ├── config/
    │   └── Config.java            # Runtime configuration
    ├── database/
    │   ├── DatabaseManager.java   # SQLite connection lifecycle
    │   └── FileRepository.java    # Data access layer (CRUD)
    ├── indexer/
    │   ├── Crawler.java           # Interface — directory crawling
    │   ├── DirectoryCrawler.java  # Implementation — recursive file discovery
    │   ├── Extractor.java         # Implementation — metadata & content extraction
    │   ├── FileExtractor.java     # Interface — file extraction
    │   ├── FileRecord.java        # Record — indexed file data model
    │   ├── FileUtils.java         # Utility — file extension parsing
    │   ├── IndexingReport.java    # Indexing statistics & report generation
    │   └── IndexingService.java   # Full indexing pipeline
    ├── search/
    │   ├── Formatter.java         # Interface — result formatting
    │   ├── QueryProcessor.java    # Raw query → FTS5 query transformation
    │   ├── ResultFormatter.java   # Implementation — readable output
    │   ├── SearchEngine.java      # Interface — search abstraction
    │   ├── SearchRepository.java  # SQL execution against FTS5
    │   ├── SearchResult.java      # Record — search result data model
    │   └── SearchService.java     # Implementation — wires processor + repository
    ├── ui/
    │   ├── CLI.java               # Command-line interface
    │   └── GUI.java               # Swing graphical interface
    └── Main.java                  # Application entry point
```

---

## Architecture Overview

The project is organized into four packages, each with a clear responsibility:

- **`config`** — Holds all runtime settings that control how the system behaves.
- **`database`** — Manages the SQLite connection and provides all database operations.
- **`indexer`** — Handles crawling directories, extracting file data, and coordinating the indexing pipeline.
- **`search`** — Handles query processing, database querying via FTS5, and result formatting.
- **`ui`** — Provides the CLI and GUI through which users interact with the system.

Interfaces are used to abstract the crawling (`Crawler`), extraction (`FileExtractor`), searching (`SearchEngine`), and formatting (`Formatter`), allowing each component to be developed and tested independently.

---

## Component Breakdown

### config

**`Config`** — Stores all runtime configuration with sensible defaults. Fields include root directory (defaults to `~/Documents`), max search results (50), report format (`text` or `json`), a set of ignored directory names (e.g., `node_modules`, `.git`, `build`), and a list of ignored file extensions (e.g., `.exe`, `.dll`, `.class`). Provides standard getters and setters, plus convenience methods `setIgnoredExtensionsFromString()` and `setIgnoredDirsFromString()` that parse comma-separated strings from the GUI into proper collections. All settings can be changed at runtime through the GUI before triggering a re-index.

---

### database

**`DatabaseManager`** — Manages the SQLite connection lifecycle. On connect, it enables **WAL (Write-Ahead Logging)** journal mode so that reads and writes can happen concurrently, and sets a **busy timeout of 5 seconds** so operations retry instead of failing immediately when the database is momentarily locked. Provides `ensureConnected()` for automatic reconnection if the connection drops.

**`FileRepository`** — The data access layer. Responsible for:
- **Schema initialization**: Creates the `files` table with columns for all metadata (path, name, extension, size, timestamps, flags, MIME type, tags, content, preview), an **FTS5 virtual table** (`files_fts`) indexing the `name` and `content` columns, and **triggers** that keep the FTS5 index in sync on insert, update, and delete operations.
- **CRUD operations**: `insertOrUpdate()` performs an insert or update (on path conflict), `getLastModified()` retrieves a file's stored timestamp for incremental indexing, `getAllIndexedPaths()` returns all indexed paths for stale-file detection, `removeByPath()` deletes a record, and `clearDatabase()` deletes everything.

---

### indexer

**`Crawler`** *(interface)* — Defines the contract for directory crawling: `crawl(rootPath)` returns a list of discovered file paths, and `getSkippedCount()` reports how many files were inaccessible.

**`DirectoryCrawler`** *(implements Crawler)* — Walks the file tree recursively using `Files.walkFileTree`. During traversal it: skips directories in the ignore list (e.g., `node_modules`, `.git`), detects and avoids **symlink loops** by comparing resolved real paths, filters out files with ignored extensions, and tracks files that couldn't be accessed (permission denied, broken links) using `visitFileFailed`.

**`FileExtractor`** *(interface)* — Defines a single method `extract(Path)` that returns a `FileRecord`. Abstracts the extraction logic so it could be swapped for a different implementation.

**`Extractor`** *(implements FileExtractor)* — For each file, it:
1. Reads filesystem attributes (size, creation time, last modified time, hidden/readable flags).
2. Probes the MIME type via `Files.probeContentType`.
3. Assigns semantic **tags** based on file extension (e.g., `.java` → `code`, `.json` → `config`, `.csv` → `data`).
4. For recognized text file extensions, reads the content using a **charset fallback chain** (UTF-8 → Windows-1250 → Windows-1252 → ISO-8859-2) and generates a **preview** from the first 3 non-blank lines.
5. Detects and skips binary content by counting non-printable characters.

**`FileRecord`** *(record)* — An immutable data class holding all metadata about an indexed file: path, name, extension, size, lastModified, createdAt, isHidden, isReadable, mimeType, tags, content, and preview.

**`FileUtils`** — A utility class with a single static method `getExtension()` that extracts the file extension from a filename.

**`IndexingReport`** — Tracks indexing statistics (indexed, unchanged, skipped, failed, removed counts) and measures elapsed time from construction. Its `generate()` method produces either a formatted text report or a JSON report, depending on the configured report format.

**`IndexingService`** — The orchestrator that runs the full indexing pipeline:
1. Creates a `DirectoryCrawler` and `Extractor`.
2. Crawls the configured root directory.
3. For each discovered file, compares its `lastModifiedTime` against the stored value — **unchanged files are skipped** (incremental indexing).
4. Changed or new files are extracted and updated/inserted into the database.
5. After processing, detects **files deleted from disk** by comparing current paths against all indexed paths, and removes stale records.
6. Accumulates skipped counts from the crawler.
7. Returns the generated report.

---

### search

**`SearchEngine`** *(interface)* — Defines a single method `search(rawQuery)` that returns a list of `SearchResult`. Abstracts the search mechanism.

**`QueryProcessor`** — Transforms raw user input into an FTS5-compatible query string:
- Empty or blank input → returns empty string.
- Single word (e.g., `hello`) → `hello*`
- Multiple words (e.g., `hello world`) → `hello AND world` (all terms must match).

**`SearchRepository`** — Executes the processed query against the database. The SQL joins the `files` table with `files_fts` using `MATCH`, which searches across **both the filename and the file content** simultaneously. Results are grouped by name and size to avoid duplicates.

**`SearchResult`** *(record)* — An immutable data class holding the fields returned from a search: path, name, extension, size, lastModified, tags, and preview.

**`Formatter`** *(interface)* — Defines a single method `format(List<SearchResult>)` that converts search results into a displayable string.

**`ResultFormatter`** *(implements Formatter)* — Builds a readable string for each result showing: rank number, filename, full path, type/extension, formatted size (B/KB/MB), last-modified date, and content preview, separated by horizontal lines.

**`SearchService`** *(implements SearchEngine)* — Wires together `QueryProcessor` and `SearchRepository`. Processes the raw query, checks if it's blank, and delegates to the repository. Acts as the single entry point used by both the CLI and GUI.

---

### ui

**`Main`** — The application entry point. Initializes the database connection, schema, configuration, repositories, services, and launches either the CLI or GUI.

**`CLI`** — A terminal-based search interface. Runs a loop that prompts the user for queries, passes them through `SearchService`, formats results with `ResultFormatter`, and prints them to the console. Typing `exit` quits the application.

**`GUI`** — A Swing-based graphical interface with three sections:
- **Configuration panel** (top): Editable fields for root directory, ignored extensions, max results, and report format, plus a Re-Index button.
- **Search bar** (middle): Text field with a `DocumentListener` that triggers a search on **every keystroke**, providing live-as-you-type results.
- **Results area** (center): A scrollable, monospaced text area displaying formatted search results.
- **Status bar** (bottom): Shows current state (ready, result count, re-indexing status).

Re-indexing runs on a background `SwingWorker` thread to keep the UI responsive, and the button is disabled during the operation to prevent concurrent re-indexes.

---

## How It Works

### Indexing Pipeline

```
Config (root dir, ignore rules)
    │
    ▼
DirectoryCrawler
    │  Walks file tree recursively
    │  Skips ignored dirs, symlink loops, ignored extensions
    │  Tracks access failures
    │
    ▼
Extractor (for each discovered file)
    │  Reads attributes (size, timestamps, flags)
    │  Probes MIME type
    │  Assigns tags by extension
    │  Reads text content with charset fallback
    │  Generates 3-line preview
    │  Skips binary files
    │
    ▼
IndexingService (orchestrator)
    │  Compares lastModified → skips unchanged files
    │  Upserts new/modified files via FileRepository
    │  Removes deleted files from database
    │  Generates indexing report
    │
    ▼
SQLite Database
    ├── files table (all metadata + content)
    └── files_fts (FTS5 index on name + content)
        └── Kept in sync via INSERT/UPDATE/DELETE triggers
```

### Search Pipeline

```
User types query
    │
    ▼
QueryProcessor
    │  "hello"       → "hello*"         (prefix match)
    │  "hello world" → "hello AND world" (all terms required)
    │
    ▼
SearchRepository
    │  SELECT ... FROM files JOIN files_fts
    │  WHERE files_fts MATCH ?
    │  → Searches BOTH filename AND content
    │  → Ordered by FTS5 relevance rank
    │
    ▼
ResultFormatter
    │  Formats each result with name, path, type, size, date, preview
    │
    ▼
CLI prints to console / GUI displays in text area
```



## Technologies Used

- **Java 17+** — Records, text blocks, switch expressions, `Files.walkFileTree`
- **SQLite** — Lightweight embedded database via JDBC
- **FTS5** — SQLite's full-text search extension for fast content and filename search
- **Swing** — Java's built-in GUI toolkit for the graphical interface
