package com.devoxx.genie.ui.compose.model

data class FeatureDoc(val emoji: String, val name: String, val tooltip: String, val url: String)

val FEATURES = listOf(
    FeatureDoc("\uD83D\uDD12", "Security Scanning", "Scan your code for vulnerabilities and security issues", "https://genie.devoxx.com/docs/features/security-scanning"),
    FeatureDoc("\uD83D\uDCCB", "Spec-driven Dev", "Generate code from OpenAPI and other specification files", "https://genie.devoxx.com/docs/features/spec-driven-development"),
    FeatureDoc("\uD83E\uDD16", "Agent Mode", "Let the AI autonomously plan and execute multi-step tasks", "https://genie.devoxx.com/docs/features/agent-mode"),
    FeatureDoc("\uD83D\uDCBB", "CLI Runners", "Execute shell commands directly from the AI conversation", "https://genie.devoxx.com/docs/features/cli-runners"),
    FeatureDoc("\uD83D\uDD17", "ACP Runners", "Connect to Agent Communication Protocol services", "https://genie.devoxx.com/docs/features/acp-runners"),
    FeatureDoc("\u2699\uFE0F", "MCP Support", "Extend capabilities with Model Context Protocol servers", "https://genie.devoxx.com/docs/features/mcp_expanded"),
    FeatureDoc("\uD83C\uDFAF", "Skills", "Use predefined prompt templates for common tasks", "https://genie.devoxx.com/docs/features/skills"),
    FeatureDoc("\uD83C\uDFA8", "Appearance", "Customize fonts, colors, and layout to your liking", "https://genie.devoxx.com/docs/configuration/appearance"),
    FeatureDoc("\uD83D\uDDBC\uFE0F", "DnD Images", "Drag and drop images into the chat for visual context", "https://genie.devoxx.com/docs/features/dnd-images"),
    FeatureDoc("\uD83D\uDCDA", "RAG Support", "Index your project for semantic search with ChromaDB", "https://genie.devoxx.com/docs/features/rag"),
    FeatureDoc("\uD83D\uDCAC", "Chat History", "Persist and restore conversations across sessions", "https://genie.devoxx.com/docs/features/chat-memory"),
    FeatureDoc("\uD83D\uDD0D", "Project Scanner", "Analyze project structure and add context to prompts", "https://genie.devoxx.com/docs/features/project-scanner"),
    FeatureDoc("\u270D\uFE0F", "Inline Completion", "Get AI code completions directly in the editor", "https://genie.devoxx.com/docs/features/inline-completion"),
    FeatureDoc("\uD83C\uDF10", "Web Search", "Augment prompts with live web search results", "https://genie.devoxx.com/docs/features/web-search"),
)
