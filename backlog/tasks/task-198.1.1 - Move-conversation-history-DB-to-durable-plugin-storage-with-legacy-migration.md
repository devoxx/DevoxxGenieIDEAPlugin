---
id: TASK-198.1.1
title: Move conversation history DB to durable plugin storage with legacy migration
status: Done
assignee: []
created_date: '2026-03-08 15:54'
updated_date: '2026-03-08 15:58'
labels:
  - persistence
  - storage
  - migration
dependencies: []
references:
  - >-
    src/main/java/com/devoxx/genie/service/conversations/ConversationStorageService.java
  - src/main/java/com/devoxx/genie/service/ChatService.java
  - >-
    src/main/java/com/devoxx/genie/ui/panel/conversationhistory/ConversationHistoryPanel.java
documentation:
  - >-
    https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
  - >-
    https://www.jetbrains.com/help/idea/directories-used-by-the-ide-to-store-settings-caches-plugins-and-logs.html
  - 'https://www.jetbrains.com/help/idea/invalidate-caches.html'
parent_task_id: TASK-198.1
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Conversation history is currently stored under the IDE system/cache area, which is not appropriate for durable user data. Move the conversation history database into durable plugin or user config storage so histories survive cache invalidation and system directory cleanup, and provide a safe migration path for developers who already have history data in the legacy location.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Conversation history is stored in durable plugin or user config storage rather than the IDE system/cache path.
- [x] #2 On first run after the change, an existing conversation-history database in the legacy location is migrated automatically without requiring manual user steps.
- [x] #3 Existing migrated conversation history remains readable after migration, and new conversations continue to be saved in the new location.
- [x] #4 If no legacy database exists, startup and conversation history behavior continue to work normally without migration errors.
- [x] #5 The migration is safe to run multiple times and does not duplicate or corrupt conversation history data.
- [x] #6 Automated tests cover the storage-location selection and legacy migration behavior.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Changes

### Storage location change
- `ConversationStorageService` constructor now uses `PathManager.getConfigPath()` instead of `PathManager.getSystemPath()`
- Config path is durable: survives \"Invalidate Caches\" and system directory cleanup
  - macOS: `~/Library/Application Support/JetBrains/IntelliJIdea2024.3/DevoxxGenie/conversations.db`
  - Linux: `~/.config/JetBrains/IntelliJIdea2024.3/DevoxxGenie/conversations.db`
  - Windows: `%APPDATA%\JetBrains\IntelliJIdea2024.3\DevoxxGenie\conversations.db`

### Legacy migration
- New `migrateLegacyDatabase(Path durablePath)` static method
- Runs once during construction, before table creation
- Copies the legacy DB file from system/cache path to config path only when:
  1. Legacy file exists at old location
  2. New file does NOT yet exist at config location
- Idempotent: second+ runs are no-ops since the durable file already exists
- Non-fatal: if copy fails, logs a warning and lets a fresh DB be created

### Tests
- `ConversationStorageServiceTest` (4 tests):
  - Legacy exists + durable doesn't → migration occurs
  - Legacy exists + durable exists → no overwrite
  - Legacy doesn't exist → no error, durable remains absent
  - Idempotent: second run after migration doesn't overwrite
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Moved conversation history DB from volatile `PathManager.getSystemPath()` (IDE cache) to durable `PathManager.getConfigPath()` (IDE config). Added automatic one-time migration that copies the legacy DB file on first run. Migration is idempotent and non-fatal. 4 automated tests cover the migration scenarios.
<!-- SECTION:FINAL_SUMMARY:END -->
