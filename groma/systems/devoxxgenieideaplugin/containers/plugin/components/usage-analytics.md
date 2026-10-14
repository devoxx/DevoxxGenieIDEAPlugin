---
type: C4 Component
title: Usage analytics
status: stable
groma:
  id: usage-analytics
  parent: plugin
  code:
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/AnalyticsConsentNotifier.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/AnalyticsEventBuilder.java
      symbol: AnalyticsEventBuilder
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java
      symbol: AnalyticsService
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/AnalyticsSessionSnapshotService.java
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/Buckets.java
      symbol: Buckets
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/DevoxxGenieSettingsChangedTopic.java
      symbol: DevoxxGenieSettingsChangedTopic
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/FeatureId.java
      symbol: FeatureId
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/FeatureUsageTracker.java
      symbol: FeatureUsageTracker
    - scanner: java
      file: src/main/java/com/devoxx/genie/service/analytics/ProviderType.java
      symbol: ProviderType
  group: Settings and platform
  technology: GA4 Measurement Protocol
description: Anonymous, consent-gated provider/model usage telemetry
---

Shows a one-time consent notice, buckets usage into coarse features and sends fire-and-forget GA4 events with no prompt text, code, paths or identity.

## Relationships

| Source | Target | Description | Technology |
| --- | --- | --- | --- |
| [src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java](../../../../../../src/main/java/com/devoxx/genie/service/analytics/AnalyticsService.java) | [usage-analytics-endpoint](../../../../../externals/usage-analytics-endpoint.md) | Sends usage events | HTTPS POST |
