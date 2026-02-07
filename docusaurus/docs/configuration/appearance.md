---
sidebar_position: 3
title: Appearance Settings
description: Customize the look and feel of the DevoxxGenie chat interface — colors, spacing, and font sizes.
keywords: [devoxxgenie, appearance, theme, colors, fonts, customization, intellij plugin]
---

# Appearance Settings

DevoxxGenie lets you customize the visual appearance of the chat interface. You can change message colors, adjust spacing, and override font sizes to match your preferences.

## Accessing Appearance Settings

1. Open IntelliJ IDEA settings
2. Navigate to **Tools** > **DevoxxGenie** > **Appearance**

The settings are organized into three tabs: **Colors**, **Spacing**, and **Fonts**.

## Colors

The Colors tab lets you override the default theme colors for chat messages.

### Enabling Custom Colors

Toggle **Override theme colors** to enable custom color settings. When disabled, DevoxxGenie uses colors derived from your IDE theme.

### Available Color Options

| Setting | Description | Default |
|---------|-------------|---------|
| User message text | Text color for your messages | Theme default |
| User message background | Background color for your messages | Theme default |
| User message border | Left border accent for your messages | `#FF5400` (Devoxx orange) |
| Assistant message text | Text color for LLM responses | Theme default |
| Assistant message background | Background color for LLM responses | Theme default |
| Assistant message border | Left border accent for LLM responses | `#0095C9` (Devoxx blue) |

Click any color swatch to open a color picker. Colors are specified in hex format (e.g., `#FF5400`).

## Spacing

The Spacing tab controls the layout dimensions of chat messages.

| Setting | Range | Default | Description |
|---------|-------|---------|-------------|
| Line height | 0.1 - 3.0 | 1.6 | Multiplier for line height within messages |
| Padding | 0 - 50 px | 10 | Internal padding inside each message bubble |
| Margin | 0 - 50 px | 10 | External margin between messages |
| Border width | 0 - 10 px | 4 | Width of the left accent border |
| Rounded corners | on/off | on | Whether message bubbles have rounded corners |
| Corner radius | 0 - 20 px | 4 | Radius of rounded corners (when enabled) |

## Fonts

The Fonts tab lets you override the default font sizes used in the chat interface.

| Setting | Range | Default | Description |
|---------|-------|---------|-------------|
| Override editor font size | on/off | off | Enable to set a custom font size for message text |
| Custom font size | 8 - 24 px | IDE default | Font size for regular message text |
| Override code font size | on/off | off | Enable to set a custom font size for code blocks |
| Custom code font size | 8 - 24 px | IDE default | Font size for code blocks in responses |

## Resetting to Defaults

Each tab respects the global **Reset** action in the settings dialog. Click **Reset** to restore all appearance settings to their defaults.

## Tips

- Start with the default theme colors and only override if you want a distinct look
- If code blocks are hard to read, try overriding the code font size to a larger value
- Increase padding and margin for a more spacious chat layout, or decrease them to fit more content on screen
- The border accent colors are a good way to visually distinguish user and assistant messages at a glance
