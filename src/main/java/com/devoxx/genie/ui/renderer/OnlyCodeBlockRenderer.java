package com.devoxx.genie.ui.renderer;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.HashSet;
import java.util.Set;

public class OnlyCodeBlockRenderer implements NodeRenderer {

    private final HtmlWriter htmlWriter;

    public OnlyCodeBlockRenderer(HtmlNodeRendererContext context) {
        this.htmlWriter = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        HashSet<Class<? extends Node>> types = new HashSet<>();
        types.add(FencedCodeBlock.class); // only handle fenced code blocks
        return types;
    }

    @Override
    public void render(Node node) {
        if (node instanceof FencedCodeBlock) {
            FencedCodeBlock codeBlock = (FencedCodeBlock) node;
            htmlWriter.line();
            htmlWriter.raw("<div class='codeHTML'>");
            htmlWriter.tag("pre");
            htmlWriter.text(codeBlock.getLiteral());
            htmlWriter.tag("/pre");
            htmlWriter.raw("</div>");
            htmlWriter.line();
        }
    }

    public static void main(String[] args) {
        String markdown = """
            # Hello World\n\n
            This is a paragraph with `inline code`.\n\n
            ```java\n
            public static void main(String[] args) {\n
                System.out.println(\"Hello, World!\");
                \n
            }\n
            ```
        """;

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);

        HtmlRenderer renderer = HtmlRenderer.builder()
            .nodeRendererFactory(OnlyCodeBlockRenderer::new)
            .build();

        String html = renderer.render(document);

        System.out.println(html);
    }
}
