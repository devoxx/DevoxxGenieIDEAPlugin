---
sidebar_position: 2
title: Installation Guide - DevoxxGenie Documentation
description: A comprehensive guide to installing the DevoxxGenie plugin in IntelliJ IDEA, including prerequisites, installation methods, and troubleshooting tips.
keywords: [devoxxgenie, intellij plugin, installation, jetbrains marketplace, plugin installation, setup]
image: /img/devoxxgenie-social-card.jpg
---

# Installation Guide

This guide will help you install the DevoxxGenie plugin in your IntelliJ IDEA environment.

## Prerequisites

Before installing DevoxxGenie, ensure you have the following:

- **IntelliJ IDEA**: Version 2023.3.4 or later
- **Java**: JDK 17 or later

## Installation Methods

There are two ways to install the DevoxxGenie plugin:

### Method 1: From JetBrains Marketplace (Recommended)

1. Open IntelliJ IDEA
2. Go to `Settings` (or `Preferences` on macOS)
3. Navigate to `Plugins` > `Marketplace`
4. Search for "Devoxx" or "DevoxxGenie"
5. Click the `Install` button next to the DevoxxGenie plugin
6. Restart IntelliJ IDEA when prompted

![Installing from Marketplace](/img/provider-selection.png)

### Method 2: Manual Installation

If you prefer to install the plugin manually or need a specific version:

1. Download the plugin ZIP file from the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/24169-devoxxgenie) or from the [GitHub Releases page](https://github.com/devoxx/DevoxxGenieIDEAPlugin/releases)
2. In IntelliJ IDEA, go to `Settings` (or `Preferences` on macOS)
3. Navigate to `Plugins`
4. Click the gear icon and select `Install Plugin from Disk...`
5. Locate and select the downloaded ZIP file
6. Click `OK` and restart IntelliJ IDEA when prompted

## Verifying Installation

After installation, you should see the DevoxxGenie icon in the right toolbar of your IDE. Click on the icon to open the DevoxxGenie tool window.

![DevoxxGenie Tool Window](/img/devoxxgenie-toolwindow.png)

## Troubleshooting Installation Issues

### Plugin Not Visible After Installation

If you don't see the DevoxxGenie icon after installation:

1. Ensure you've restarted IntelliJ IDEA after installation
2. Check if the plugin is enabled in `Settings` > `Plugins` > `Installed`
3. Try invalidating caches and restarting by going to `File` > `Invalidate Caches / Restart...`

### Version Compatibility Issues

If you encounter compatibility issues:

1. Verify your IntelliJ IDEA version is 2023.3.4 or later
2. Check if your Java version is JDK 17 or later
3. Check the plugin description for any specific compatibility notes

### Next Steps

Once you've successfully installed DevoxxGenie, you can:

- [Set up with local LLM models](quick-start-local.md)
- [Set up with cloud LLM services](quick-start-cloud.md)
- [Explore the features](../features/overview.md)
