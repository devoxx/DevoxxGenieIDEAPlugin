package com.devoxx.genie.ui.listener;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Listener interface for handling file references events.
 * Used to notify components when file references are available for a specific chat message.
 */
public interface FileReferencesListener {
    /**
     * Called when file references are available for a chat message.
     * 
     * @param chatMessageContext The chat message context
     * @param files The list of files referenced in the chat
     */
    void onFileReferencesAvailable(@NotNull ChatMessageContext chatMessageContext, @NotNull List<VirtualFile> files);
}
