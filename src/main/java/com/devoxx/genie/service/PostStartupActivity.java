package com.devoxx.genie.service;

import com.devoxx.genie.service.prompt.memory.ChatMemoryManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class PostStartupActivity implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ChatMemoryManager chatMemoryManager = ChatMemoryManager.getInstance();
        if (chatMemoryManager != null) {
            chatMemoryManager.initializeMemory(project);
        } else {
            log.error("chatMemoryManager is null");
        }

        return continuation;
    }
}
