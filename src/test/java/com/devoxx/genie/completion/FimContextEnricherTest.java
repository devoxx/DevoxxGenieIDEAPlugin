package com.devoxx.genie.completion;

import com.intellij.psi.PsiElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FimContextEnricherTest {

    @Test
    void declarationText_stripsJavadocAndAnnotationsAndCompacts() {
        PsiElement element = mock(PsiElement.class);
        when(element.getText()).thenReturn("""
                /**
                 * Adds two numbers.
                 */
                @Override
                public int add(int a,
                               int b) {
                    return a + b;
                }""");
        assertThat(FimContextEnricher.declarationText(element))
                .isEqualTo("public int add(int a, int b)");
    }

    @Test
    void declarationText_cutsAtColonForIndentationLanguages() {
        PsiElement element = mock(PsiElement.class);
        when(element.getText()).thenReturn("""
                @staticmethod
                def add(a, b):
                    return a + b""");
        assertThat(FimContextEnricher.declarationText(element)).isEqualTo("def add(a, b):");
    }

    @Test
    void declarationText_handlesElementWithoutBrace() {
        PsiElement element = mock(PsiElement.class);
        when(element.getText()).thenReturn("public static final int MAX = 10;");
        assertThat(FimContextEnricher.declarationText(element))
                .isEqualTo("public static final int MAX = 10;");
    }

    @Test
    void buildContext_returnsBasePrefixWhenNoContexts() {
        assertThat(FimContextEnricher.buildContext(List.of(), "prefix", 100, 4)).isEqualTo("prefix");
    }

    @Test
    void buildContext_returnsBasePrefixWhenBudgetIsZero() {
        var ctx = new FimContextEnricher.ClassContext("a.A", "class A", List.of());
        assertThat(FimContextEnricher.buildContext(List.of(ctx), "prefix", 0, 4)).isEqualTo("prefix");
    }

    @Test
    void buildContext_prependsNeutralLanguageAgnosticHeader() {
        var ctx = new FimContextEnricher.ClassContext("com.example.Calculator",
                "public class Calculator", List.of("public int add(int a, int b)"));
        String result = FimContextEnricher.buildContext(List.of(ctx), "prefix", 1000, 4);
        assertThat(result).startsWith(
                "class: com.example.Calculator\npublic class Calculator\npublic int add(int a, int b)\n");
        assertThat(result).endsWith("prefix");
    }

    @Test
    void buildContext_respectsMaxClasses() {
        var ctx1 = new FimContextEnricher.ClassContext("a.A", "class A", List.of());
        var ctx2 = new FimContextEnricher.ClassContext("b.B", "class B", List.of());
        String result = FimContextEnricher.buildContext(List.of(ctx1, ctx2), "prefix", 1000, 1);
        assertThat(result).contains("class: a.A").doesNotContain("class: b.B");
        assertThat(result).endsWith("prefix");
    }

    @Test
    void buildContext_respectsBudget() {
        var ctx = new FimContextEnricher.ClassContext("a.A", "class A", List.of("int x = 1;"));
        String result = FimContextEnricher.buildContext(List.of(ctx), "prefix", 20, 4);
        assertThat(result).startsWith("class: a.A\n");
        assertThat(result).endsWith("prefix");
        assertThat(result.length() - "prefix".length()).isEqualTo(20);
    }
}
