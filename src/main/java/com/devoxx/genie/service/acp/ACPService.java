package com.devoxx.genie.service.acp;

import com.devoxx.genie.model.acp.ACPSettings;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;

/**
 * Static utility for ACP (Agent Client Protocol) state queries.
 * Mirrors the MCPService pattern.
 */
public final class ACPService {

    private ACPService() {
        // Utility class
    }

    /**
     * Check if ACP is enabled and an agent command is configured.
     */
    public static boolean isACPEnabled() {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        ACPSettings settings = state.getAcpSettings();
        return Boolean.TRUE.equals(state.getAcpEnabled())
                && settings != null
                && !settings.getAgentCommand().isEmpty();
    }

    /**
     * Get the current ACP settings.
     */
    public static ACPSettings getACPSettings() {
        return DevoxxGenieStateService.getInstance().getAcpSettings();
    }
}
