# DEVOXXGENIE.md

## Project Guidelines

### Build Commands

- **Build:** `./gradlew build`
- **Test:** `./gradlew test`
- **Single Test:** `./gradlew test --tests ClassName.methodName`
- **Clean:** `./gradlew clean`
- **Run:** `./gradlew run`

### Code Style

- **Formatting:** Use IDE or checkstyle for formatting
- **Naming:**
  - Use camelCase for variables, methods, and fields
  - Use PascalCase for classes and interfaces
  - Use SCREAMING_SNAKE_CASE for constants
- **Documentation:** Use JavaDoc for documentation
- **Imports:** Organize imports and avoid wildcard imports
- **Exception Handling:** Prefer specific exceptions and document throws



### Project Tree

```
DevoxxGenieIDEAPlugin/
  src/
    main/
      java/
      kotlin/
      resources/
    test/
      java/
      kotlin/
      resources/
  docs/
    superpowers/
      plans/
      specs/
  groma/
    flows/
      answer-with-rag-context.md
      ask-the-llm-in-agent-mode.md
      run-spec-tasks-with-a-cli-agent.md
      trigger-a-prompt-from-an-ide-event.md
    actors/
      developer.md
    systems/
      devoxxgenieideaplugin/
    index.md
    externals/
      docker.md
      algolia.md
      mailjet.md
      chromadb.md
      mcp-servers.md
      mcp-registry.md
      web-search-apis.md
      coding-agent-clis.md
      intellij-platform.md
      local-llm-runtimes.md
      project-stats-apis.md
      cloud-llm-providers.md
      other-intellij-plugins.md
      security-scanner-tools.md
      docker-agentic-platform.md
      usage-analytics-endpoint.md
    project.md
    scanners.json
  gradle/
    wrapper/
      gradle-wrapper.properties
  .claude/
    skills/
      review/
      release/
      start-task/
      git-commit-push-pr/
      close-task-commit-push-pr/
  .github/
    workflows/
      tests.yml
      claude.yml
      gradle.yml
      link-check.yml
      deploy-docs.yml
      test-deploy-docs.yml
      claude-code-review.yml
    ISSUE_TEMPLATE/
      config.yml
      bug_report.md
      feature_request.md
    dependabot.yaml
    PULL_REQUEST_TEMPLATE.md
  .gradle/
    9.9.1/
      expanded/
      checksums/
      fileHashes/
      fileChanges/
      vcsMetadata/
      gc.properties
      executionHistory/
      kotlin-dsl-plugin-entries/
    vcs-1/
      gc.properties
    buildOutputCleanup/
      cache.properties
  .kotlin/
    sessions/
  backlog/
    tasks/
      task-214 - Add-a-cross-encoder-reranker-stage.md
      task-216 - RAG-health-and-diagnostics-UI-panel.md
      task-240 - Enable-Ollama-thinking-from-settings.md
      task-273 - Add-Rapid-MLX-as-a-local-LLM-provider.md
      task-277.2 - Show-source-links-in-search-answers.md
      task-213 - Add-BM25-hybrid-retrieval-with-RRF-merge.md
      task-212 - Make-the-RAG-embedding-model-configurable.md
      task-260 - Prevent-Activity-Log-from-freezing-the-IDE.md
      task-274.3 - Serve-Jan-from-the-generic-local-provider.md
      task-215 - AST-aware-code-chunking-via-language-analyzers.md
      task-274.1 - Serve-GPT4All-from-the-generic-local-provider.md
      task-239 - Fix-kotlinS3776-in-ActivitySectionkt-at-line-134.md
      task-277.6 - Record-web-search-usage-analytics-per-provider.md
      task-277.3 - Add-Keenable-as-an-optional-web-search-provider.md
      task-277.5 - Add-Parallel-as-an-optional-web-search-provider.md
      task-254 - Show-agent-tool-activity-during-streaming-responses.md
      task-262 - Refresh-token-cost-and-context-window-documentation.md
      task-237 - Chat-output-font-zoom-via-CMD-Ctrl-keyboard-shortcuts.md
      task-264 - Retire-unused-success-stories-page-and-promotion-links.md
      task-276 - Make-DevoxxGenie-settings-load-on-IntelliJ-IDEA-2026.3.md
      task-263 - Update-DevoxxGenie-pricing-comparison-and-privacy-claims.md
      task-261.1 - Document-crucial-gaps-in-recent-plugin-feature-coverage.md
      task-197 - Replace-deprecated-Compose-APIs-reported-by-plugin-verifier.md
      task-261 - Align-Docusaurus-reference-documentation-with-plugin-1.15.1.md
      task-257 - Let-users-edit-built-in-agent-tool-descriptions-from-Settings.md
      task-267 - RAG-settings-validation-must-not-run-network-calls-on-the-EDT.md
      task-274 - Serve-GPT4All-Jan-and-LLaMA.c-from-the-generic-local-provider.md
      task-195 - Fix-Links-in-chat-output-must-be-clickable-and-open-in-browser.md
      task-211 - Add-persistent-semantic-conversation-memory-backed-by-ChromaDB.md
      task-218 - RAG-query-expansion-fix-misleading-one-extra-LLM-call-help-text.md
      task-226 - Scheduled-cron-agent-automations-for-unattended-recurring-tasks.md
      task-275 - Show-the-full-error-in-the-chats-error-card-and-let-users-copy-it.md
      task-219 - Make-RAG-mutually-exclusive-with-MCP-and-Agent-mode-in-settings-UI.md
      task-210 - GenieBuilder-admin-UI-Feature-Usage-panel-for-DevoxxGenie-analytics.md
      task-189 - Prompt-Queue-Allow-submitting-prompts-while-another-is-still-processing.md
      task-255 - Mid-task-steering-send-chat-messages-to-a-running-agent-task-issue-1241.md
      task-274.2 - Serve-LLaMA.c-from-the-generic-local-provider-with-real-model-listing.md
      task-277.7 - Make-the-web-search-test-in-Settings-readable-and-give-clear-feedback.md
      task-228 - Additional-messaging-channel-adapters-Slack-Socket-Mode-and-Discord-gateway.md
      task-272 - Support-LLM-routers-through-one-generic-provider-and-a-Routers-settings-tab.md
      task-271 - Offer-model-downloads-for-local-embedding-providers-other-than-Ollama-optional.md
      task-277.4 - Deprecate-the-Google-Custom-Search-provider-ahead-of-its-2027-01-01-shutdown.md
      task-230 - Prototype-find_callees-PSI-tool-Java-only-spike-to-validate-call-graph-approach.md
      task-224 - Self-improving-Skill-Writer-capture-and-refine-agent-skills-from-successful-runs.md
      task-225 - Agent-task-planner-with-explicit-decomposition-and-writable-sub-agent-delegation.md
      task-277.1 - One-web-search-provider-registry-and-selection-service-used-by-every-search-path.md
      task-268 - RAG-settings-action-button-fires-its-action-multiple-times-after-repeated-validations.md
      task-277 - Pluggable-web-search-providers-one-registry-covering-current-and-future-search-engines.md
      task-236 - Long-running-feedback-and-history-polish-elapsed-ticker-determinate-progress-history-UX.md
      task-266 - Add-a-Test-button-to-RAG-settings-that-verifies-the-unsaved-RAG-configuration-end-to-end.md
      task-227 - Remote-chat-control-via-messaging-channels-Telegram-long-polling-MVP-with-adapter-abstraction.md
      task-270 - Start-Indexing-duplicates-every-chunk-when-re-indexing-a-project-with-the-same-embedding-model.md
      task-274.4 - llama.cpp-follow-ups-show-single-local-model-official-name-fix-blank-chat-on-empty-streamed-reply.md
      task-265 - Support-OpenAI-compatible-embedding-endpoints-with-query-document-prefixes-and-dimension-truncation-for-RAG.md
    archive/
      tasks/
    completed/
      task-259 - Give-each-code-block-its-own-syntax-highlighting-builder.md
      task-269 - Add-a-repeatable-RAG-retrieval-quality-benchmark-to-RagCli.md
      task-252 - Gracefully-handle-missing-API-key-for-selected-cloud-LLM-provider.md
      task-253 - Block-saving-LLM-settings-when-an-enabled-cloud-provider-has-no-API-key.md
      task-207 - GenieBuilder-surface-DevoxxGenie-prompt_executed-events-in-analytics-dashboard.md
      task-258 - Fix-crash-rendering-code-blocks-whose-text-contains-an-unmatched-comment-terminator.md
    config.yml
    milestones/
      m-0 - rag-configurable-embeddings-(#523).md
  META-INF/
    core.xml
    ej-core.xml
  AGENTS.md
  CLAUDE.md
  GEMINI.md
  README.md
  docusaurus/
    src/
      css/
      pages/
      theme/
      plugins/
      components/
      clientModules/
    blog/
      authors.yml
      2026-08-09-personas.md
      2026-02-13-cli-runners.md
      2026-02-14-acp-runners.md
      2026-02-22-security-scanning.md
      2026-07-01-nvidia-free-models.md
      2026-10-28-devoxxgenie-1-15-0.md
      2026-11-03-devoxxgenie-1-16-0.md
      2026-11-12-devoxxgenie-1-18-0.md
      2026-05-18-commands-and-skills.md
      2026-07-02-see-the-llm-thinking.md
      2026-11-03-run-script-code-mode.md
      2026-04-10-local-ai-clusters-exo.md
      2026-07-19-cloudflare-ai-gateway.md
      2026-07-19-run-command-blacklist.md
      2026-02-10-spec-driven-development.md
      2026-11-04-rag-embedding-providers.md
      2026-02-09-devoxxgenie-goes-agentic.md
      2026-02-18-devoxxgenie-integrations.md
      2026-06-04-rag-semantic-search-tool.md
      2026-06-15-engram-persistent-memory.md
      2026-08-10-share-your-success-story.md
      2026-08-11-agent-changed-files-diff.md
      2026-08-24-editable-tool-descriptions.md
      2026-06-30-devoxxgenie-plugin-analytics.md
      2026-07-07-devoxxgenie-growth-milestone.md
      2026-07-09-mtplx-local-llm-apple-silicon.md
      2026-07-29-nativ-local-mlx-apple-silicon.md
      2026-11-18-devoxxgenie-1-19-0-web-search.md
      2025-03-24-devoxxgenie-assistant-for-idea.md
      2026-11-02-matt-pocock-addy-osmani-agent-skills.md
    docs/
      features/
      contributing/
      integrations/
      configuration/
      llm-providers/
      getting-started/
    static/
      api/
      img/
      robots.txt
    .claude/
      settings.local.json
    scripts/
      README.md
      fix-image-paths.js
      markdown-files.txt
      update-metadata.js
    README.md
    WEBSITE.md
    sidebars.js
    .clever.json
    package.json
    docusaurus.config.js
    placeholder-setup.js
    create-placeholders.js
    generate-png-placeholders.js
  CHANGELOG.md
  Taskfile.yml
  DEVOXXGENIE.md
  CONTRIBUTING.md
  .intellijPlatform/
    sandbox/
      DevoxxGenie/
    localPlatformArtifacts/
      IU-263.7307.200/
      IU-253.31033.145/
  gradle.properties
  TASKFILE_README.md

```
