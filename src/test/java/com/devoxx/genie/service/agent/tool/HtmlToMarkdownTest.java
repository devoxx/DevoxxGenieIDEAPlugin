package com.devoxx.genie.service.agent.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlToMarkdownTest {

    private static final String PAGE = "https://docs.example.com/guide/migrate";

    private static String convert(String body) {
        return HtmlToMarkdown.convert("<html><head><title>Ignored title</title></head><body>" + body + "</body></html>", PAGE);
    }

    @Test
    void headingsBecomeAtxHeadingsAndTheTitleIsLeftOut() {
        var markdown = convert("<h1>Migrating</h1><p>Intro</p><h2>Before</h2><p>Old way</p>");

        assertThat(markdown).isEqualTo("# Migrating\n\nIntro\n\n## Before\n\nOld way");
    }

    @Test
    void codeBlockKeepsItsLinesAndLanguageWhenEachLineIsASeparateElement() {
        var markdown = convert("<div class=\"language-java codeBlockContainer\"><pre class=\"prism-code language-java\"><code>"
                + "<span class=\"token-line\">client.required(\"city\") // explicit</span><br>"
                + "<span class=\"token-line\">    .build();</span><br></code></pre></div><p>Next paragraph</p>");

        assertThat(markdown).isEqualTo("```java\nclient.required(\"city\") // explicit\n    .build();\n```\n\nNext paragraph");
    }

    @Test
    void codeBlockContainingBackticksGetsALongerFence() {
        var markdown = convert("<pre>```inner```</pre>");

        assertThat(markdown).isEqualTo("````\n```inner```\n````");
    }

    @Test
    void tableKeepsRowsAndColumns() {
        var markdown = convert("<table><thead><tr><th>Old</th><th>New</th></tr></thead><tbody>"
                + "<tr><td>connect()</td><td>open()</td></tr>"
                + "<tr><td>setTimeout(int)</td><td>timeout(Duration)</td></tr></tbody></table>");

        assertThat(markdown).isEqualTo("| Old | New |\n| --- | --- |\n| connect() | open() |\n| setTimeout(int) | timeout(Duration) |");
    }

    @Test
    void tableCellsEscapePipesAndShortRowsArePadded() {
        var markdown = convert("<table><tr><th>A</th><th>B</th></tr><tr><td>x | y</td></tr></table>");

        assertThat(markdown).isEqualTo("| A | B |\n| --- | --- |\n| x \\| y |  |");
    }

    @Test
    void strikethroughKeepsTheCrossedOutMeaning() {
        var markdown = convert("<p>Price drops <del>$30</del> $20</p>");

        assertThat(markdown).isEqualTo("Price drops ~~$30~~ $20");
    }

    @Test
    void relativeLinksAreResolvedAgainstThePageUrl() {
        var markdown = convert("<p>See the <a href=\"../api/client.html\">API reference</a>.</p>");

        assertThat(markdown).isEqualTo("See the [API reference](https://docs.example.com/api/client.html).");
    }

    @Test
    void aUrlIsWrittenOnlyTheFirstTimeItIsLinked() {
        var markdown = convert("<p><a href=\"/api\">API</a>, <a href=\"/api\">the API</a> and <a href=\"/faq\">FAQ</a></p>");

        assertThat(markdown).isEqualTo("[API](https://docs.example.com/api), the API and [FAQ](https://docs.example.com/faq)");
    }

    @Test
    void imagesAreLeftOut() {
        var markdown = convert("<p>Usage <img src=\"/img/chart.png\" alt=\"Usage chart\">today</p>");

        assertThat(markdown).isEqualTo("Usage today");
    }

    @Test
    void anchorsToTheSamePageKeepOnlyTheirText() {
        var markdown = convert("<h2 id=\"tools\">Tools<a href=\"#tools\" class=\"hash-link\">​</a></h2>"
                + "<p>Jump to <a href=\"#setup\">setup</a> or <a href=\"" + PAGE + "#faq\">FAQ</a>.</p>");

        assertThat(markdown).isEqualTo("## Tools\n\nJump to setup or FAQ.");
    }

    @Test
    void inlineFormattingKeepsTheSpacesAroundIt() {
        var markdown = convert("<p>The <code>connect()</code> method is <strong>gone</strong> in <em>2.0</em>.</p>");

        assertThat(markdown).isEqualTo("The `connect()` method is **gone** in *2.0*.");
    }

    @Test
    void nestedListsAreIndentedUnderTheirItem() {
        var markdown = convert("<ul><li>Java 17<ul><li>LTS</li></ul></li><li>Gradle 8</li></ul>"
                + "<ol start=\"3\"><li>Third</li><li>Fourth</li></ol>");

        assertThat(markdown).isEqualTo("- Java 17\n\n  - LTS\n- Gradle 8\n\n3. Third\n4. Fourth");
    }

    @Test
    void blockquoteLinesArePrefixed() {
        var markdown = convert("<blockquote><p>First</p><p>Second</p></blockquote>");

        assertThat(markdown).isEqualTo("> First\n>\n> Second");
    }

    @Test
    void scriptsStylesNavigationFootersAndHiddenElementsAreDropped() {
        var markdown = convert("<script>track()</script><style>.x{}</style><nav><a href=\"/\">Home</a></nav>"
                + "<p>Visible</p><p hidden>hidden attribute</p><p style=\"display: none\">display none</p>"
                + "<span aria-hidden=\"true\">icon</span><footer>Privacy · Terms</footer>");

        assertThat(markdown).isEqualTo("Visible");
    }

    @Test
    void mainContentIsPreferredOverTheSurroundingPage() {
        var markdown = convert("<header><p>Site banner</p></header><div class=\"sidebar\">Sidebar</div>"
                + "<main><h1>Guide</h1><p>Body</p></main>");

        assertThat(markdown).isEqualTo("# Guide\n\nBody");
    }

    @Test
    void lineBreaksInsideAParagraphAreKept() {
        var markdown = convert("<p>first line<br>second line</p>");

        assertThat(markdown).isEqualTo("first line\nsecond line");
    }
}
