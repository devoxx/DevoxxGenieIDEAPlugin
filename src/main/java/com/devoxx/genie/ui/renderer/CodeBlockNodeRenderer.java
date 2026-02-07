package com.devoxx.genie.ui.renderer;

import com.devoxx.genie.ui.util.LanguageGuesser;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.richcopy.HtmlSyntaxInfoUtil;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.editor.colors.EditorColorsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Special commonmark renderer for code blocks.
 * IJ has a nifty utility HtmlSyntaxInfoUtil that is used to copy
 * selected code as HTML it also happens this utility is used to generate
 * the HTML for the code blocks in the documentation.
 * Use this way
 * HtmlRenderer renderer = HtmlRenderer.builder()
 * .nodeRendererFactory(context -> new CodeBlockNodeRenderer(project, context))
 * .build();
 * String html = renderer.render(node);
 */
public class CodeBlockNodeRenderer implements NodeRenderer {
    private static final float DEFAULT_HIGHLIGHTING_SATURATION = 1.0f;
    private final Project project;
    private final HtmlWriter htmlOutputWriter;

    public CodeBlockNodeRenderer(Project project, @NotNull HtmlNodeRendererContext context) {
        this.project = project;
        this.htmlOutputWriter = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        return Set.of(IndentedCodeBlock.class, FencedCodeBlock.class, Code.class);
    }

    @Override
    public void render(Node node) {
        if (node instanceof IndentedCodeBlock indentedCodeBlock) {
            renderNode(indentedCodeBlock.getLiteral(), true);
        } else if (node instanceof FencedCodeBlock fencedCodeBlock) {
            renderNode(fencedCodeBlock.getLiteral(), fencedCodeBlock.getInfo(), true);
        } else if (node instanceof Code code) {
            renderNode(code.getLiteral(), false);
        }
    }

    private void renderNode(String codeSnippet, String info, boolean block) {
        htmlOutputWriter.line();

        if (block) {
            // Style to ensure proper whitespace handling
            Map<String, String> preStyle = new HashMap<>();
            preStyle.put("style", "-webkit-user-select: text; user-select: text; white-space: pre !important;");
            htmlOutputWriter.tag("pre", preStyle);
        }

        // Critical: we need to handle newlines properly in the code tag
        Map<String, String> codeStyle = new HashMap<>();
        // Use editor font size instead of hardcoded value and account for IDE scale factor
        int editorFontSize = EditorColorsManager.getInstance().getGlobalScheme().getEditorFontSize();

        // Use JBUI.scale to account for IDE zoom/scaling settings
        int scaledFontSize = com.intellij.util.ui.JBUI.scale(editorFontSize);
        codeStyle.put("style", "font-size:" + scaledFontSize + "pt; white-space: pre-wrap !important; overflow-x: auto; -webkit-user-select: text; user-select: text;");

        htmlOutputWriter.tag("code", codeStyle);

        HighlightingMode highlightingMode = determineHighlightingMode(block);
        Language language = LanguageGuesser.guessLanguage(info != null ? info.trim().toLowerCase() : "");
        if (language == null) {
            language = PlainTextLanguage.INSTANCE;
        }

        // Get highlighted code
        String highlightedCode = getHighlightedCode(codeSnippet, language, highlightingMode);

        // Process the code to ensure proper line breaks
        String processedCode = highlightedCode
                .replace("&#32;", " ")                                  // Convert space entities to spaces
                .replaceAll("</span><span[^>]*><br></span>", "\n")      // Convert br tags to newlines
                .replace("<br>", "\n")                                  // Handle any remaining br tags
                .replace("\n", "</span>\n<span>");                      // Ensure spans wrap around newlines

        htmlOutputWriter.raw("<span>" + processedCode + "</span>");

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

    private @NotNull String getHighlightedCode(String codeSnippet, Language language, HighlightingMode mode) {
        StringBuilder highlightedCode = new StringBuilder();
        if (mode == HighlightingMode.SEMANTIC_HIGHLIGHTING) {
            ApplicationManager.getApplication().runReadAction((Computable<StringBuilder>) () ->
                    HtmlSyntaxInfoUtil.appendHighlightedByLexerAndEncodedAsHtmlCodeSnippet(
                            highlightedCode,
                            project,
                            language,
                            codeSnippet,
                            false,
                            DEFAULT_HIGHLIGHTING_SATURATION
                    )
            );
        } else {
            highlightedCode.append(StringUtil.escapeXmlEntities(codeSnippet));
        }
        return highlightedCode.toString();
    }

    private HighlightingMode determineHighlightingMode(boolean block) {
        if (block) {
            return HighlightingMode.SEMANTIC_HIGHLIGHTING;
        } else {
            return HighlightingMode.INLINE_HIGHLIGHTING;
        }
    }
}