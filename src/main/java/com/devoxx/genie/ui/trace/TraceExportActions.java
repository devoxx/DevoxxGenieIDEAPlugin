package com.devoxx.genie.ui.trace;

import com.devoxx.genie.model.conversation.Conversation;
import com.devoxx.genie.service.conversations.ConversationStorageService;
import com.devoxx.genie.service.trace.TraceSettings;
import com.devoxx.genie.service.trace.hf.HfUploadSettings;
import com.devoxx.genie.service.trace.hf.TraceUploadService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * "Preview as trace" and "Export as trace…" for the conversation history panel (task-280).
 * Both use the same conversion, second redaction pass and TruffleHog {@code required} hold-back
 * as the Hugging Face upload, so what you preview or export is exactly what would be uploaded.
 */
public final class TraceExportActions {

    private TraceExportActions() {}

    /** Opens the conversation's STS JSONL in a read-only editor tab. Call on the EDT. */
    public static void preview(@NotNull Project project, @NotNull Conversation conversation) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<TraceUploadService.HeldBack> heldBack = new ArrayList<>();
            List<TraceUploadService.Prepared> prepared = prepare(project, List.of(conversation), heldBack);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                if (prepared.isEmpty()) {
                    NotificationUtil.sendWarningNotification(project, "No trace to preview: "
                            + (heldBack.isEmpty() ? "nothing recorded" : heldBack.get(0).reason()) + ".");
                    return;
                }
                LightVirtualFile file = new LightVirtualFile(conversation.getId() + ".jsonl",
                        prepared.get(0).sts().jsonl());
                file.setWritable(false);
                FileEditorManager.getInstance(project).openFile(file, true);
            });
        });
    }

    /** Asks for a folder and writes one {@code <conversationId>.jsonl} per conversation. Call on the EDT. */
    public static void export(@NotNull Project project, @NotNull List<Conversation> conversations) {
        if (conversations.isEmpty()) {
            return;
        }
        VirtualFile folder = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Export Agent Traces")
                        .withDescription("Choose a folder for the Session Trace (.jsonl) files"),
                project, null);
        if (folder == null) {
            return;
        }
        Path dir = folder.toNioPath();
        new Task.Backgroundable(project, "Exporting agent traces", true) {
            private int written;
            private final List<TraceUploadService.HeldBack> heldBack = new ArrayList<>();
            private String error;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    for (TraceUploadService.Prepared p : prepare(project, conversations, heldBack)) {
                        indicator.checkCanceled();
                        Path file = dir.resolve(Path.of(p.path()).getFileName().toString());
                        Files.writeString(file, p.sts().jsonl(), StandardCharsets.UTF_8);
                        written++;
                    }
                } catch (IOException e) {
                    error = e.getMessage();
                }
            }

            @Override
            public void onFinished() {
                if (error != null) {
                    NotificationUtil.sendErrorNotification(project, "Trace export failed: " + error);
                    return;
                }
                String held = heldBack.isEmpty() ? "" : " " + heldBack.size() + " held back ("
                        + heldBack.get(0).reason() + (heldBack.size() > 1 ? ", …" : "") + ").";
                NotificationUtil.sendNotification(project, "Exported " + written + " trace file(s) to " + dir + "." + held);
                folder.refresh(true, false);
            }
        }.queue();
    }

    /** Uploads the conversations to Hugging Face (see {@link HfTraceUploader}). Call on the EDT. */
    public static void upload(@NotNull Project project, @NotNull List<Conversation> conversations) {
        HfTraceUploader.upload(project, conversations, () -> {}, () -> {});
    }

    private static List<TraceUploadService.Prepared> prepare(@NotNull Project project,
                                                             @NotNull List<Conversation> conversations,
                                                             @NotNull List<TraceUploadService.HeldBack> heldBack) {
        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        return TraceUploadService.prepare(conversations,
                id -> ConversationStorageService.getInstance().getTraceEvents(id),
                HfUploadSettings.fromState(state), TraceSettings.fromState(state), project.getName(), heldBack);
    }
}
