package com.devoxx.genie.ui.settings;

import com.devoxx.genie.service.DevoxxGenieSettingsService;
import com.devoxx.genie.service.DevoxxGenieSettingsServiceProvider;
import com.intellij.openapi.application.ApplicationManager;

public class DevoxxGenieStateServiceProvider implements DevoxxGenieSettingsServiceProvider {

    @Override
    public DevoxxGenieSettingsService getDevoxxGenieSettingsService() {
        return ApplicationManager.getApplication().getService(DevoxxGenieStateService.class);
    }
}
