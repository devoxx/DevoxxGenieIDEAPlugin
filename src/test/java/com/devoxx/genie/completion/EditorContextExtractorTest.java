package com.devoxx.genie.completion;

import com.intellij.openapi.editor.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EditorContextExtractorTest {

    @Mock
    private Document document;

    @Test
    void shouldExtractPrefixAndSuffix() {
        String text = "public class Foo {\n    int x = 10;\n}";
        when(document.getText()).thenReturn(text);

        int offset = 20; // inside the class body
        EditorContextExtractor result = EditorContextExtractor.extract(document, offset);

        assertThat(result.getPrefix()).isEqualTo(text.substring(0, 20));
        assertThat(result.getSuffix()).isEqualTo(text.substring(20));
    }

    @Test
    void shouldHandleEmptyDocument() {
        when(document.getText()).thenReturn("");

        EditorContextExtractor result = EditorContextExtractor.extract(document, 0);

        assertThat(result.getPrefix()).isEmpty();
        assertThat(result.getSuffix()).isEmpty();
    }

    @Test
    void shouldHandleCursorAtStart() {
        String text = "hello world";
        when(document.getText()).thenReturn(text);

        EditorContextExtractor result = EditorContextExtractor.extract(document, 0);

        assertThat(result.getPrefix()).isEmpty();
        assertThat(result.getSuffix()).isEqualTo("hello world");
    }

    @Test
    void shouldHandleCursorAtEnd() {
        String text = "hello world";
        when(document.getText()).thenReturn(text);

        EditorContextExtractor result = EditorContextExtractor.extract(document, text.length());

        assertThat(result.getPrefix()).isEqualTo("hello world");
        assertThat(result.getSuffix()).isEmpty();
    }

    @Test
    void shouldClampOffsetBeyondDocumentLength() {
        String text = "short";
        when(document.getText()).thenReturn(text);

        EditorContextExtractor result = EditorContextExtractor.extract(document, 999);

        assertThat(result.getPrefix()).isEqualTo("short");
        assertThat(result.getSuffix()).isEmpty();
    }

    @Test
    void shouldClampNegativeOffset() {
        String text = "hello";
        when(document.getText()).thenReturn(text);

        EditorContextExtractor result = EditorContextExtractor.extract(document, -5);

        assertThat(result.getPrefix()).isEmpty();
        assertThat(result.getSuffix()).isEqualTo("hello");
    }

    @Test
    void shouldTruncateLongPrefix() {
        // Create a document larger than MAX_PREFIX_CHARS (4096)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append('x');
        }
        String text = sb.toString();
        when(document.getText()).thenReturn(text);

        EditorContextExtractor result = EditorContextExtractor.extract(document, 5000);

        // Prefix should be capped at 4096 chars
        assertThat(result.getPrefix()).hasSize(4096);
        assertThat(result.getSuffix()).isEmpty();
    }

    @Test
    void shouldTruncateLongSuffix() {
        // Create a document larger than MAX_SUFFIX_CHARS (1024)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append('y');
        }
        String text = sb.toString();
        when(document.getText()).thenReturn(text);

        EditorContextExtractor result = EditorContextExtractor.extract(document, 0);

        assertThat(result.getPrefix()).isEmpty();
        // Suffix should be capped at 1024 chars
        assertThat(result.getSuffix()).hasSize(1024);
    }
}
