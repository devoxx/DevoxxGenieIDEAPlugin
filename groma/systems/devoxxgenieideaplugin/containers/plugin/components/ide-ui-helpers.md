---
type: C4 Component
title: IDE UI helpers
status: stable
groma:
  id: ide-ui-helpers
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/CJKFontUtil.java
      symbol: CJKFontUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/CodeLanguageUtil.java
      symbol: CodeLanguageUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/CodeSnippetAction.java
      symbol: CodeSnippetAction
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/DevoxxGenieColorsUtil.java
      symbol: DevoxxGenieColorsUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/DevoxxGenieFontsUtil.java
      symbol: DevoxxGenieFontsUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/DevoxxGenieIconsUtil.java
      symbol: DevoxxGenieIconsUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/DoubleConverterUtil.java
      symbol: DoubleConverterUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/EditorUtil.java
      symbol: EditorUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/FileTypeIconUtil.java
      symbol: FileTypeIconUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/FontUtil.java
      symbol: FontUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/HelpUtil.java
      symbol: HelpUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/LanguageGuesser.java
      symbol: LanguageGuesser
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/NotificationUtil.java
      symbol: NotificationUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/SettingsDialogUtil.java
      symbol: SettingsDialogUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/ThemeChangeListener.java
      symbol: ThemeChangeListener
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/ThemeChangeNotifier.java
      symbol: ThemeChangeNotifier
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/ThemeDetector.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/TimestampUtil.java
      symbol: TimestampUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/WindowContextFormatterUtil.java
      symbol: WindowContextFormatterUtil
    - scanner: java
      file: src/main/java/com/devoxx/genie/ui/util/WindowPluginUtil.java
      symbol: WindowPluginUtil
  group: Settings and platform
  technology: Swing, IntelliJ UI
description: Theme, font, icon, notification and editor helpers
---

Shared Swing/IDE helpers used across panels: theme change detection, fonts (including CJK), colours, icons, notifications, editor and settings-dialog utilities.
