package com.devoxx.genie.service.analytics;

import java.util.Set;

/** Test-only bridge to the package-private analytics allowlist, for wiring tests in other packages. */
public final class AnalyticsEventBuilderAccess {

    private AnalyticsEventBuilderAccess() {
    }

    public static Set<String> allowedFeatureIds() {
        return AnalyticsEventBuilder.ENUM_VALUE_ALLOWLIST.get("feature_id");
    }
}
