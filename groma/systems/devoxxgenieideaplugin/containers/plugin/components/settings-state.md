---
type: C4 Component
title: Settings state and credentials
status: stable
groma:
  id: settings-state
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/credentials/CredentialKey.java
      symbol: CredentialKey
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/credentials/CredentialService.java
      symbol: CredentialService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialService.java
      symbol: PasswordSafeCredentialService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/DevoxxGenieSettingsService.java
      symbol: DevoxxGenieSettingsService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/PropertiesService.java
      symbol: PropertiesService
    - scanner: java
      file: src/main/java/com/devoxx/genie/startup/CredentialMigrationStartupActivity.java
      symbol: CredentialMigrationStartupActivity
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/settings/DevoxxGenieStateService.java
  group: Settings and platform
  technology: IntelliJ PersistentStateComponent, PasswordSafe
description: Persistent plugin settings and secret storage
---

`DevoxxGenieStateService` is the single persisted state for all feature toggles, provider URLs, limits and preferences. API keys and tokens live in PasswordSafe through `CredentialService`; a startup activity migrates keys that were stored in plain settings.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialService.java](../../../../../../src/main/java/com/devoxx/genie/service/credentials/PasswordSafeCredentialService.java) | [intellij-platform](../../../../../externals/intellij-platform.md) | Stores secrets | PasswordSafe API |
