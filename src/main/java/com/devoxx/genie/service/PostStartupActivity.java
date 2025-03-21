package com.devoxx.genie.service;

import com.devoxx.genie.service.mcp.MCPService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PostStartupActivity implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(PostStartupActivity.class);

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ChatMemoryService chatMemoryService = ChatMemoryService.getInstance();
        if (chatMemoryService != null) {
            chatMemoryService.init(project);
        } else {
            LOG.error("ChatMemoryService is null");
        }

        return continuation;
    }
}
