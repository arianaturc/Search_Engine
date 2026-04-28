package test;

import search.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QueryProcessor Tests")
class QueryProcessorTest {

    private QueryProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new QueryProcessor();
    }


    @Test
    @DisplayName("Unqualified words become general terms")
    void generalTermsParsing() {
        QueryProcessor.ParsedQuery result = processor.parse("hello world");
        assertEquals(List.of("hello", "world"), result.getGeneralTerms());
        assertTrue(result.getPathTerms().isEmpty());
        assertTrue(result.getContentTerms().isEmpty());
    }

    @Test
    @DisplayName("Qualifiers are parsed into correct categories")
    void qualifierParsing() {
        QueryProcessor.ParsedQuery result = processor.parse("path:src content:hello ext:.java tag:code");
        assertEquals(List.of("src"), result.getPathTerms());
        assertEquals(List.of("hello"), result.getContentTerms());
        assertEquals(List.of(".java"), result.getExtTerms());
        assertEquals(List.of("code"), result.getTagTerms());
        assertTrue(result.getGeneralTerms().isEmpty());
    }

    @Test
    @DisplayName("Duplicate qualifiers are combined (AND semantics)")
    void duplicateQualifiers() {
        QueryProcessor.ParsedQuery result = processor.parse("path:src path:main content:test content:config");
        assertEquals(List.of("src", "main"), result.getPathTerms());
        assertEquals(List.of("test", "config"), result.getContentTerms());
    }

    @Test
    @DisplayName("FTS query joins multiple terms with AND")
    void ftsQueryBuilding() {
        String result = processor.buildFtsQuery(List.of("hello", "world"));
        assertTrue(result.contains("hello") && result.contains("AND") && result.contains("world"));
    }

}