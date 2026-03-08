package com.devoxx.genie.ui.compose.components

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for code block rendering to verify braces are preserved.
 * Reproduces TASK-198: Java code blocks rendered without opening or closing braces.
 */
class ExtractCodeTextTest {

    private val parser = MarkdownParser(GFMFlavourDescriptor())

    /**
     * Reproduces the extractCodeText() logic from AiBubble.kt (which is private).
     */
    private fun extractCodeText(content: String, node: ASTNode): String {
        val codeLines = mutableListOf<String>()
        for (child in node.children) {
            when (child.type) {
                MarkdownTokenTypes.CODE_FENCE_CONTENT ->
                    codeLines.add(content.substring(child.startOffset, child.endOffset))
                MarkdownTokenTypes.EOL ->
                    if (codeLines.isNotEmpty()) codeLines.add("\n")
            }
        }
        if (codeLines.isNotEmpty()) {
            return codeLines.joinToString("").trimEnd()
        }

        // For indented code blocks, collect CODE_LINE children
        for (child in node.children) {
            if (child.type == MarkdownTokenTypes.CODE_LINE) {
                codeLines.add(content.substring(child.startOffset, child.endOffset))
            } else if (child.type == MarkdownTokenTypes.EOL) {
                if (codeLines.isNotEmpty()) codeLines.add("\n")
            }
        }
        if (codeLines.isNotEmpty()) {
            return codeLines.joinToString("").trimEnd()
        }

        // Fallback: use the full node text minus the fence markers
        val fullText = content.substring(node.startOffset, node.endOffset)
        return fullText
            .removePrefix("```").substringAfter('\n')
            .removeSuffix("```").trimEnd()
    }

    /**
     * Reproduces the MarkdownCodeFence extraction logic from mikepenz library.
     */
    private fun extractCodeFenceMikepenz(content: String, node: ASTNode): String? {
        if (node.children.size >= 3) {
            val start = node.children[2].startOffset
            val end = node.children[(node.children.size - 2).coerceAtLeast(2)].endOffset
            return content.subSequence(start, end).toString().replaceIndent()
        }
        return null
    }

    private fun findCodeFenceNode(content: String): ASTNode? {
        val tree = parser.buildMarkdownTreeFromString(content)
        return findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)
    }

    private fun findNodeOfType(node: ASTNode, type: org.intellij.markdown.IElementType): ASTNode? {
        if (node.type == type) return node
        for (child in node.children) {
            val found = findNodeOfType(child, type)
            if (found != null) return found
        }
        return null
    }

    @Test
    fun `java class with braces should preserve opening and closing braces`() {
        val markdown = """
            |Here is a Java class:
            |
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello, World!");
            |    }
            |}
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node, "Code fence node should be found")

        val extractedText = extractCodeText(markdown, node!!)
        assertTrue(extractedText.contains("{"), "Extracted text should contain opening brace: $extractedText")
        assertTrue(extractedText.contains("}"), "Extracted text should contain closing brace: $extractedText")
        assertTrue(extractedText.contains("public class HelloWorld {"), "Should contain class declaration with brace")
        assertTrue(extractedText.trimEnd().endsWith("}"), "Should end with closing brace")

        val mikepenzText = extractCodeFenceMikepenz(markdown, node)
        assertNotNull(mikepenzText, "Mikepenz extraction should succeed")
        assertTrue(mikepenzText!!.contains("{"), "Mikepenz text should contain opening brace: $mikepenzText")
        assertTrue(mikepenzText.contains("}"), "Mikepenz text should contain closing brace: $mikepenzText")

        // Print AST for debugging
        printAst(markdown, node, 0)
    }

    @Test
    fun `record with inline braces should be preserved`() {
        val markdown = """
            |```java
            |public record Point(int x, int y) {}
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node)

        val extractedText = extractCodeText(markdown, node!!)
        assertTrue(extractedText.contains("{}"), "Should contain empty braces: $extractedText")
    }

    @Test
    fun `code starting with opening brace should be preserved`() {
        val markdown = """
            |```java
            |{
            |    "key": "value"
            |}
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node)

        val extractedText = extractCodeText(markdown, node!!)
        assertTrue(extractedText.startsWith("{"), "Should start with opening brace: $extractedText")
        assertTrue(extractedText.trimEnd().endsWith("}"), "Should end with closing brace: $extractedText")
    }

    @Test
    fun `switch expression with braces should be preserved`() {
        val markdown = """
            |```java
            |String result = switch (day) {
            |    case MONDAY -> {
            |        yield "Start";
            |    }
            |    default -> "Other";
            |};
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node)

        val extractedText = extractCodeText(markdown, node!!)
        val braceCount = extractedText.count { it == '{' }
        assertEquals(2, braceCount, "Should have 2 opening braces: $extractedText")
        val closeBraceCount = extractedText.count { it == '}' }
        assertEquals(2, closeBraceCount, "Should have 2 closing braces: $extractedText")
    }

    @Test
    fun `code fence without language should preserve braces`() {
        val markdown = """
            |```
            |public class Foo {
            |    int x = 0;
            |}
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node)

        val extractedText = extractCodeText(markdown, node!!)
        assertTrue(extractedText.contains("{"), "Should contain opening brace: $extractedText")
        assertTrue(extractedText.contains("}"), "Should contain closing brace: $extractedText")
    }

    @Test
    fun `nested braces should all be preserved`() {
        val markdown = """
            |```java
            |public class Outer {
            |    class Inner {
            |        void method() {
            |            if (true) {
            |                // nested
            |            }
            |        }
            |    }
            |}
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node)

        val extractedText = extractCodeText(markdown, node!!)
        val openCount = extractedText.count { it == '{' }
        val closeCount = extractedText.count { it == '}' }
        assertEquals(4, openCount, "Should have 4 opening braces: $extractedText")
        assertEquals(4, closeCount, "Should have 4 closing braces: $extractedText")
    }

    @Test
    fun `streaming partial code fence without closing should preserve braces`() {
        // During streaming, the code fence might not have closing markers yet
        val markdown = """
            |Here is a class:
            |
            |```java
            |public class HelloWorld {
            |    public static void main(String[] args) {
            |        System.out.println("Hello");
        """.trimMargin()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        println("=== Streaming partial AST ===")
        printAst(markdown, tree, 0)

        // Check that braces aren't lost even in partial parsing
        val fullText = markdown
        val codeFence = findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)
        if (codeFence != null) {
            val extractedText = extractCodeText(markdown, codeFence)
            println("Extracted from partial fence: '$extractedText'")
            assertTrue(extractedText.contains("{"), "Partial code fence should contain opening brace: $extractedText")
        } else {
            println("No code fence found in partial markdown - parser sees it as:")
            tree.children.forEach { child ->
                val text = markdown.substring(child.startOffset, child.endOffset)
                    .replace("\n", "\\n").take(80)
                println("  ${child.type}: '$text'")
            }
            // Even if not parsed as code fence, the full text should still contain braces
            println("Full markdown text contains { : ${fullText.contains("{")}")
        }
    }

    @Test
    fun `code block with only braces on separate lines`() {
        val markdown = """
            |```java
            |{
            |}
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node, "Code fence should be found")

        val extractedText = extractCodeText(markdown, node!!)
        println("Extracted: '$extractedText'")
        assertTrue(extractedText.contains("{"), "Should contain opening brace: $extractedText")
        assertTrue(extractedText.contains("}"), "Should contain closing brace: $extractedText")

        val mikepenzText = extractCodeFenceMikepenz(markdown, node)
        println("Mikepenz: '$mikepenzText'")
        assertNotNull(mikepenzText)
        assertTrue(mikepenzText!!.contains("{"), "Mikepenz should contain opening brace: $mikepenzText")
        assertTrue(mikepenzText.contains("}"), "Mikepenz should contain closing brace: $mikepenzText")
    }

    @Test
    fun `verify AST structure for java code with braces`() {
        val markdown = """
            |```java
            |public class Foo {
            |}
            |```
        """.trimMargin()

        val tree = parser.buildMarkdownTreeFromString(markdown)
        println("=== Full AST ===")
        printAst(markdown, tree, 0)

        val codeFence = findNodeOfType(tree, MarkdownElementTypes.CODE_FENCE)
        assertNotNull(codeFence)

        // Check that all children include brace content
        println("\n=== Code Fence Children ===")
        for ((i, child) in codeFence!!.children.withIndex()) {
            val text = markdown.substring(child.startOffset, child.endOffset)
                .replace("\n", "\\n")
            println("  [$i] ${child.type}: '$text'")
        }
    }

    @Test
    fun `braces in paragraph text should be preserved by parser`() {
        // This simulates what happens during streaming when code fence is incomplete
        val markdown = "public class Foo {\n    void bar() {\n    }\n}"

        val tree = parser.buildMarkdownTreeFromString(markdown)
        println("=== Braces in paragraph AST ===")
        printAst(markdown, tree, 0)

        // The full text should still contain all braces
        val fullRendered = StringBuilder()
        collectAllText(markdown, tree, fullRendered)
        println("Collected text: '$fullRendered'")
        val openBraces = fullRendered.count { it == '{' }
        val closeBraces = fullRendered.count { it == '}' }
        assertEquals(2, openBraces, "Should have 2 opening braces in: $fullRendered")
        assertEquals(2, closeBraces, "Should have 2 closing braces in: $fullRendered")
    }

    @Test
    fun `incomplete code fence during streaming should handle braces`() {
        // Simulate streaming where we have opening ``` but content is still coming
        val markdown = "```java\npublic class Foo {\n    void bar() {"

        val tree = parser.buildMarkdownTreeFromString(markdown)
        println("=== Incomplete fence AST ===")
        printAst(markdown, tree, 0)

        val fullRendered = StringBuilder()
        collectAllText(markdown, tree, fullRendered)
        println("Collected text: '$fullRendered'")
        assertTrue(fullRendered.contains("{"), "Should contain opening braces")
    }

    @Test
    fun `double braces in code should be preserved`() {
        val markdown = """
            |```java
            |Map<String, String> map = new HashMap<>() {{
            |    put("key", "value");
            |}};
            |```
        """.trimMargin()

        val node = findCodeFenceNode(markdown)
        assertNotNull(node)

        val extractedText = extractCodeText(markdown, node!!)
        println("Extracted: '$extractedText'")
        assertTrue(extractedText.contains("{{"), "Should contain double opening braces: $extractedText")
        assertTrue(extractedText.contains("}}"), "Should contain double closing braces: $extractedText")
    }

    private fun collectAllText(content: String, node: ASTNode, sb: StringBuilder) {
        if (node.children.isEmpty()) {
            sb.append(content.substring(node.startOffset, node.endOffset))
        } else {
            for (child in node.children) {
                collectAllText(content, child, sb)
            }
        }
    }

    private fun printAst(content: String, node: ASTNode, indent: Int) {
        val prefix = " ".repeat(indent)
        val text = content.substring(node.startOffset, node.endOffset)
            .replace("\n", "\\n")
            .take(60)
        println("$prefix${node.type} [${node.startOffset}..${node.endOffset}] '$text'")
        for (child in node.children) {
            printAst(content, child, indent + 2)
        }
    }
}
