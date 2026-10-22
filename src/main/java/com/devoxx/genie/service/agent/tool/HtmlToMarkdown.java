package com.devoxx.genie.service.agent.tool;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class HtmlToMarkdown {

    private static final String NON_CONTENT = String.join(", ",
            "script", "style", "noscript", "template", "iframe", "object", "embed", "svg", "canvas",
            "button", "input", "select", "textarea", "nav", "aside", "footer",
            "[hidden]", "[aria-hidden=true]",
            "[style~=(?i)display\\s*:\\s*none]", "[style~=(?i)visibility\\s*:\\s*hidden]");

    private static final Pattern CODE_LANGUAGE = Pattern.compile("(?:^|\\s)(?:language|lang)-([\\w#+.-]+)");

    private static final Pattern BACKTICK_RUN = Pattern.compile("`+");

    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200D\\uFEFF]");

    private final String pageUrlWithoutFragment;

    private final Set<String> linkedUrls = new HashSet<>();

    private HtmlToMarkdown(@NotNull String pageUrl) {
        this.pageUrlWithoutFragment = withoutFragment(pageUrl);
    }

    static @NotNull String convert(@NotNull String html, @NotNull String pageUrl) {
        var document = Jsoup.parse(html, pageUrl);
        var content = mainContent(document);
        content.select(NON_CONTENT).remove();
        return new HtmlToMarkdown(pageUrl).blocks(content).strip();
    }

    private static @NotNull Element mainContent(@NotNull Document document) {
        var main = document.selectFirst("main, [role=main]");
        if (main != null) {
            return main;
        }
        var articles = document.select("article");
        if (articles.size() == 1) {
            return articles.first();
        }
        document.body().select("body > header").remove();
        return document.body();
    }

    private @NotNull String blocks(@NotNull Element element) {
        var buffer = new MarkdownBuffer();
        renderChildren(element, buffer);
        return buffer.toString();
    }

    private @NotNull String inline(@NotNull Element element) {
        return blocks(element).replaceAll("\\s+", " ").strip();
    }

    private void renderChildren(@NotNull Element element, @NotNull MarkdownBuffer out) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode text) {
                out.text(ZERO_WIDTH.matcher(text.text()).replaceAll(""));
            } else if (child instanceof Element childElement) {
                render(childElement, out);
            }
        }
    }

    private void render(@NotNull Element element, @NotNull MarkdownBuffer out) {
        switch (element.normalName()) {
            case "h1", "h2", "h3", "h4", "h5", "h6" -> heading(element, out);
            case "pre" -> codeBlock(element, out);
            case "code", "kbd", "samp", "tt" -> inlineCode(element, out);
            case "a" -> link(element, out);
            case "strong", "b" -> emphasised(element, "**", out);
            case "em", "i" -> emphasised(element, "*", out);
            case "del", "s", "strike" -> emphasised(element, "~~", out);
            case "br" -> out.lineBreak();
            case "hr" -> out.block("---");
            case "ul", "ol" -> list(element, out);
            case "blockquote" -> out.block(prefixEveryLine(blocks(element).strip(), "> "));
            case "table" -> table(element, out);
            case "dt" -> term(element, out);
            default -> container(element, out);
        }
    }

    private void container(@NotNull Element element, @NotNull MarkdownBuffer out) {
        if (element.isBlock()) {
            out.blockBoundary();
            renderChildren(element, out);
            out.blockBoundary();
        } else {
            renderChildren(element, out);
        }
    }

    private void heading(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var text = inline(element);
        if (!text.isEmpty()) {
            var level = element.normalName().charAt(1) - '0';
            out.block("#".repeat(level) + " " + text);
        }
    }

    private void codeBlock(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var code = element.wholeText().replaceAll("^\\n+|\\s+$", "");
        if (code.isBlank()) {
            return;
        }
        var fence = "`".repeat(Math.max(3, longestBacktickRun(code) + 1));
        out.block(fence + languageOf(element) + "\n" + code + "\n" + fence);
    }

    private void inlineCode(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var code = element.text().strip();
        if (code.isEmpty()) {
            return;
        }
        var fence = "`".repeat(longestBacktickRun(code) + 1);
        var padding = code.startsWith("`") || code.endsWith("`") ? " " : "";
        withSurroundingSpaces(element, out, fence + padding + code + padding + fence);
    }

    private void link(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var label = inline(element);
        if (label.isEmpty()) {
            return;
        }
        var target = element.absUrl("href");
        if (target.isEmpty() || target.regionMatches(true, 0, "javascript:", 0, 11) || isSamePageAnchor(target)
                || !this.linkedUrls.add(target)) {
            withSurroundingSpaces(element, out, label);
        } else if (label.equals(target)) {
            withSurroundingSpaces(element, out, target);
        } else {
            withSurroundingSpaces(element, out, "[" + label + "](" + target + ")");
        }
    }

    private void emphasised(@NotNull Element element, @NotNull String marker, @NotNull MarkdownBuffer out) {
        var text = inline(element);
        if (!text.isEmpty()) {
            withSurroundingSpaces(element, out, marker + text + marker);
        }
    }

    private void term(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var text = inline(element);
        if (!text.isEmpty()) {
            out.block("**" + text + "**");
        }
    }

    private void list(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var ordered = element.normalName().equals("ol");
        var number = ordered ? startNumberOf(element) : 0;
        var items = new StringBuilder();
        for (Element item : element.children()) {
            var body = item.normalName().equals("li") ? blocks(item).strip() : "";
            if (body.isEmpty()) {
                continue;
            }
            var marker = ordered ? (number++) + ". " : "- ";
            items.append(marker).append(indentContinuationLines(body, marker.length())).append('\n');
        }
        out.block(items.toString());
    }

    private void table(@NotNull Element element, @NotNull MarkdownBuffer out) {
        var rows = new ArrayList<List<String>>();
        for (Element row : element.select("tr")) {
            if (row.closest("table") == element) {
                var cells = row.children().stream()
                        .filter(cell -> cell.normalName().equals("th") || cell.normalName().equals("td"))
                        .map(this::cellText)
                        .toList();
                if (!cells.isEmpty()) {
                    rows.add(cells);
                }
            }
        }
        var columns = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columns <= 1) {
            container(element, out);
            return;
        }
        var markdown = new StringBuilder();
        for (int index = 0; index < rows.size(); index++) {
            markdown.append(tableRow(rows.get(index), columns)).append('\n');
            if (index == 0) {
                markdown.append("|").append(" --- |".repeat(columns)).append('\n');
            }
        }
        out.block(markdown.toString());
    }

    private @NotNull String cellText(@NotNull Element cell) {
        return inline(cell).replace("|", "\\|");
    }

    private static @NotNull String tableRow(@NotNull List<String> cells, int columns) {
        var padded = new ArrayList<>(cells);
        while (padded.size() < columns) {
            padded.add("");
        }
        return "| " + String.join(" | ", padded) + " |";
    }

    private boolean isSamePageAnchor(@NotNull String target) {
        return target.contains("#") && withoutFragment(target).equals(this.pageUrlWithoutFragment);
    }

    private static void withSurroundingSpaces(@NotNull Element element, @NotNull MarkdownBuffer out,
                                              @NotNull String markdown) {
        var original = element.wholeText();
        if (!original.isEmpty() && Character.isWhitespace(original.charAt(0))) {
            out.text(" ");
        }
        out.raw(markdown);
        if (!original.isEmpty() && Character.isWhitespace(original.charAt(original.length() - 1))) {
            out.text(" ");
        }
    }

    private static @NotNull String languageOf(@NotNull Element pre) {
        var candidates = new ArrayList<Element>();
        candidates.add(pre);
        var code = pre.selectFirst("code");
        if (code != null) {
            candidates.add(code);
        }
        pre.parents().stream().limit(2).forEach(candidates::add);
        for (Element candidate : candidates) {
            Matcher matcher = CODE_LANGUAGE.matcher(candidate.className());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private static int longestBacktickRun(@NotNull String text) {
        var longest = 0;
        var matcher = BACKTICK_RUN.matcher(text);
        while (matcher.find()) {
            longest = Math.max(longest, matcher.group().length());
        }
        return longest;
    }

    private static int startNumberOf(@NotNull Element orderedList) {
        try {
            return Integer.parseInt(orderedList.attr("start").strip());
        } catch (NumberFormatException notANumber) {
            return 1;
        }
    }

    private static @NotNull String indentContinuationLines(@NotNull String text, int width) {
        var indent = " ".repeat(width);
        var lines = text.split("\n", -1);
        var indented = new StringBuilder(lines[0]);
        for (int index = 1; index < lines.length; index++) {
            indented.append('\n');
            if (!lines[index].isEmpty()) {
                indented.append(indent).append(lines[index]);
            }
        }
        return indented.toString();
    }

    private static @NotNull String prefixEveryLine(@NotNull String text, @NotNull String prefix) {
        return text.lines()
                .map(line -> line.isEmpty() ? prefix.strip() : prefix + line)
                .collect(Collectors.joining("\n"));
    }

    private static @NotNull String withoutFragment(@NotNull String url) {
        var hash = url.indexOf('#');
        return hash < 0 ? url : url.substring(0, hash);
    }

    private static final class MarkdownBuffer {

        private final StringBuilder markdown = new StringBuilder();

        void text(@NotNull String text) {
            var value = text;
            if (atLineStart() || endsWithSpace()) {
                value = value.stripLeading();
            }
            this.markdown.append(value);
        }

        void raw(@NotNull String markdown) {
            this.markdown.append(markdown);
        }

        void lineBreak() {
            trimTrailingSpaces();
            if (!this.markdown.isEmpty() && !endsWith("\n\n")) {
                this.markdown.append('\n');
            }
        }

        void block(@NotNull String content) {
            var trimmed = content.replaceAll("^\\n+|\\s+$", "");
            if (trimmed.isBlank()) {
                return;
            }
            blockBoundary();
            this.markdown.append(trimmed);
            blockBoundary();
        }

        void blockBoundary() {
            trimTrailingSpaces();
            if (this.markdown.isEmpty()) {
                return;
            }
            while (!endsWith("\n\n")) {
                this.markdown.append('\n');
            }
        }

        private boolean atLineStart() {
            return this.markdown.isEmpty() || this.markdown.charAt(this.markdown.length() - 1) == '\n';
        }

        private boolean endsWithSpace() {
            return !this.markdown.isEmpty() && this.markdown.charAt(this.markdown.length() - 1) == ' ';
        }

        private boolean endsWith(@NotNull String suffix) {
            var length = this.markdown.length();
            return length >= suffix.length() && this.markdown.substring(length - suffix.length()).equals(suffix);
        }

        private void trimTrailingSpaces() {
            while (endsWithSpace()) {
                this.markdown.setLength(this.markdown.length() - 1);
            }
        }

        @Override
        public String toString() {
            return this.markdown.toString();
        }
    }
}
