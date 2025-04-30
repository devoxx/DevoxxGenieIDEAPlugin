# DevoxxGenie Appearance Settings

This document explains the Appearance settings feature in DevoxxGenie, which allows users to customize the look and feel of the chat interface.

## Overview

The Appearance settings feature enables users to modify various visual aspects of the DevoxxGenie chat interface, such as:

- Spacing (line height, message padding, margins)
- Colors (message borders, backgrounds)
- Font sizes (for text and code blocks)
- UI styling (rounded corners, border widths)

## User Interface

The Appearance settings are accessible through the DevoxxGenie settings panel in IntelliJ IDEA (Settings/Preferences → Tools → DevoxxGenie → Appearance).

The settings panel is organized into several tabs:

1. **General** - Basic styling options like rounded corners
2. **Colors** - Configure border and background colors
3. **Spacing** - Adjust line height, padding, and margins
4. **Fonts** - Customize font sizes for text and code blocks
5. **Preview** - See a live preview of your settings

## Technical Implementation

### Architecture

The Appearance settings feature follows a layered architecture:

1. **Settings Storage** - `DevoxxGenieStateService` stores appearance preferences
2. **UI Component** - `AppearanceSettingsComponent` provides the settings interface
3. **Apply Mechanism** - `AppearanceRefreshHandler` creates and provides CSS update scripts
4. **Notification System** - `AppearanceSettingsEvents` notifies components of changes
5. **WebView Integration** - `WebViewThemeManager` injects dynamic CSS into the WebView

### Key Components

- **`appearance-custom.css`** - CSS template with appearance variables
- **`AppearanceSettingsComponent`** - Settings UI with preview
- **`AppearanceRefreshHandler`** - Creates dynamic CSS update scripts
- **`WebServer`** - Enhanced to serve dynamic scripts

### How It Works

1. The user modifies appearance settings in the Settings panel
2. Changes are saved to `DevoxxGenieStateService`
3. The `ApplyButton` triggers the `AppearanceSettingsEvents` topic
4. `AppearanceRefreshHandler` creates a JavaScript snippet to update CSS variables
5. `WebViewThemeManager` injects the script into active WebViews
6. The script executes in the browser context, updating styles in real-time

## Extending the Feature

To add new appearance settings:

1. Add new fields to `DevoxxGenieStateService`
2. Add UI controls to `AppearanceSettingsComponent`
3. Add CSS variables to `appearance-custom.css`
4. Update the refresh logic in `AppearanceRefreshHandler`

## CSS Variables

The following CSS variables are available for customization:

- `--custom-line-height`
- `--custom-message-padding`
- `--custom-message-margin`
- `--custom-border-width`
- `--custom-corner-radius`
- `--custom-user-message-border-color`
- `--custom-assistant-message-border-color`
- `--custom-user-message-background-color`
- `--custom-assistant-message-background-color`
- `--custom-font-size`
- `--custom-code-font-size`
