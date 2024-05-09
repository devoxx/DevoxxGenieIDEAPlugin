package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.ui.util.LanguageGuesser;
import com.intellij.lang.Language;
import com.intellij.lang.documentation.DocumentationSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Collections;
import java.util.Set;

/**
 * Special commonmark renderer for code blocks.
 * IJ has a nifty utility HtmlSyntaxInfoUtil that is used to copy
 * selected code as HTML it also happens this utility is used to generate
 * the HTML for the code blocks in the documentation.
 * Use this way
 * HtmlRenderer renderer = HtmlRenderer.builder()
 *    .nodeRendererFactory(context -> new CodeBlockNodeRenderer(project, context))
 *    .build();
 * String html = renderer.render(node);
 */
public class CodeBlockNodeRenderer implements NodeRenderer {
    private final Project project;
    private final HtmlWriter htmlOutputWriter;

    public CodeBlockNodeRenderer(Project project, HtmlNodeRendererContext context) {
        this.project = project;
        this.htmlOutputWriter = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Set.of(
            IndentedCodeBlock.class, FencedCodeBlock.class, Code.class
        );
    }

    @Override
    public void render(Node node) {
        if (node instanceof IndentedCodeBlock) {
            renderNode(((IndentedCodeBlock) node).getLiteral(), true);
        } else if (node instanceof FencedCodeBlock fencedCodeBlock) {
            renderNode(fencedCodeBlock.getLiteral(), fencedCodeBlock.getInfo(), true);
        } else if (node instanceof Code code) {
            renderNode(code.getLiteral(), false);
        } else {
            System.err.println("Unknown node type: " + node);
        }
    }

    private void renderNode(String codeSnippet, String info, boolean block) {
        htmlOutputWriter.line();

        if (block) {
            htmlOutputWriter.tag("pre");
        }

        htmlOutputWriter.tag("code", Collections.singletonMap("style", "font-size:14pt"));

        HighlightingMode highlightingMode = determineHighlightingMode(block);

        htmlOutputWriter.raw(
            appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                highlightingMode,
                project,
                LanguageGuesser.guessLanguage(info) != null ? LanguageGuesser.guessLanguage(info) : PlainTextLanguage.INSTANCE,
                codeSnippet
            )
        );

        htmlOutputWriter.tag("/code");

        if (block) {
            htmlOutputWriter.tag("/pre");
        }

        htmlOutputWriter.line();
    }

    private void renderNode(String codeSnippet, boolean block) {
        renderNode(codeSnippet, "", block);
    }

    private enum HighlightingMode {
        SEMANTIC_HIGHLIGHTING, NO_HIGHLIGHTING, INLINE_HIGHLIGHTING
    }

    private HighlightingMode determineHighlightingMode(boolean block) {
        if (block && DocumentationSettings.isHighlightingOfCodeBlocksEnabled()) {
            return HighlightingMode.SEMANTIC_HIGHLIGHTING;
        } else if (block) {
            return HighlightingMode.NO_HIGHLIGHTING;
        } else {
            return HighlightingMode.INLINE_HIGHLIGHTING;
        }
    }

    private String appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
        HighlightingMode highlightingMode, Project project, Language language, String codeSnippet) {
        StringBuilder highlightedAndEncodedAsHtmlCodeSnippet = new StringBuilder();
        ApplicationManager.getApplication().runReadAction(() -> {
            if (highlightingMode == HighlightingMode.SEMANTIC_HIGHLIGHTING) {
                HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                    highlightedAndEncodedAsHtmlCodeSnippet,
                    project,
                    language,
                    codeSnippet,
                    false,
                    DocumentationSettings.getHighlightingSaturation(true)
                );
            } else {
                highlightedAndEncodedAsHtmlCodeSnippet.append(StringUtil.escapeXmlEntities(codeSnippet));
            }
        });

        if (highlightingMode != HighlightingMode.NO_HIGHLIGHTING) {
            TextAttributes codeAttributes =
                EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT).clone();
            codeAttributes.setBackgroundColor(null);

//            highlightedAndEncodedAsHtmlCodeSnippet = new StringBuilder(
//                HtmlSyntaxInfoUtil.appendStyledSpan(
//                    highlightedAndEncodedAsHtmlCodeSnippet,
//                    codeAttributes,
//                    DocumentationSettings.getHighlightingSaturation(true)
//                ));
        }

        return highlightedAndEncodedAsHtmlCodeSnippet.toString();
    }
}
