package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.service.chromadb.ChromaDBStatusCallback;
import com.devoxx.genie.service.chromadb.ChromaDockerService;
import com.devoxx.genie.service.rag.RagValidatorService;
import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.devoxx.genie.service.rag.validator.ValidationResult;
import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.devoxx.genie.service.rag.validator.ValidatorType;
import com.devoxx.genie.ui.panel.ValidatorsPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class RAGSettingsHandler implements ActionListener {
    private final Project project;
    private final JPanel validationPanel;
    private final ChromaDBManager chromaDBManager;
    private final RAGSettingsComponent settingsComponent;
    private volatile boolean validationInProgress = false;
    private volatile boolean validationQueued = false;
    private volatile boolean isInitialValidationDone = false;  // Add this flag

    public RAGSettingsHandler(Project project,
                              JPanel validationPanel,
                              RAGSettingsComponent settingsComponent) {
        this.project = project;
        this.validationPanel = validationPanel;
        this.settingsComponent = settingsComponent;
        this.chromaDBManager = ChromaDBManager.getInstance(project);
    }

    public void performValidation() {
        if (!isInitialValidationDone) {
            isInitialValidationDone = true;
        } else if (validationInProgress) {
            validationQueued = true;
            return;
        }

        // ModalityState.any() so this runs even when invoked from background callbacks
        // while the Settings dialog is open (otherwise the EDT defers it until close).
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                validationInProgress = true;

                validationPanel.removeAll();
                validationPanel.setLayout(new BoxLayout(validationPanel, BoxLayout.Y_AXIS));

                ValidationResult result = RagValidatorService.getInstance().validate();
                ValidatorsPanel statusPanel = new ValidatorsPanel(result.statuses(), this);
                validationPanel.add(statusPanel);
                validationPanel.add(Box.createVerticalStrut(5));

                validationPanel.revalidate();
                validationPanel.repaint();

                settingsComponent.updateValidationStatus();
            } finally {
                validationInProgress = false;
                if (validationQueued) {
                    validationQueued = false;
                    performValidation();
                }
            }
        }, ModalityState.any());
    }

    public void handleValidationAction(@Nullable ValidatorStatus status) {
        if (status == null || validationInProgress) {
            return;
        }

        ValidationActionType action = status.action();
        if (action == ValidationActionType.OK) {
            return;
        }

        validationInProgress = true;

        if (status.validatorType() == ValidatorType.CHROMADB) {
            handleChromaDBAction(action);
        } else if (status.validatorType() == ValidatorType.NOMIC) {
            handleNomicAction(action);
        }
    }

    private void handleNomicAction(@NotNull ValidationActionType action) {
        if (action == ValidationActionType.PULL_NOMIC) {
            pullNomicModel();
        }
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        if (!(e.getSource() instanceof JButton button)) {
            return;
        }

        ValidatorType validatorType = (ValidatorType) button.getClientProperty("validatorType");
        ValidationActionType action = (ValidationActionType) button.getClientProperty("action");

        if (validatorType == ValidatorType.CHROMADB) {
            // For the Pull Image action we replace the button with an inline progress widget
            // so the user has immediate visible feedback in the panel — the IDE's background-
            // task spinner alone is too easy to miss for a multi-minute pull.
            if (action == ValidationActionType.PULL_CHROMA_DB) {
                pullChromaDBImage(button);
            } else {
                handleChromaDBAction(action);
            }
        } else if (validatorType == ValidatorType.NOMIC) {
            handleNomicAction(action);
        }
    }

    private void pullNomicModel() {
        new Task.Backgroundable(project, "Pulling Nomic Embed Model") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);

                try {
                    OllamaModelService.getInstance().pullModel("nomic-embed-text", status -> {
                        if (status.startsWith("Downloading:")) {
                            try {
                                double progress = Double.parseDouble(status.substring(12, status.length() - 1));
                                indicator.setFraction(progress / 100.0);
                                indicator.setText(status);
                            } catch (NumberFormatException ignored) {}
                        } else {
                            indicator.setText(status);
                        }
                    });

                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.sendNotification(project, "Nomic Embed model pulled successfully");
                        performValidation();
                    }, ModalityState.any());
                } catch (IOException e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.sendNotification(project, "Failed to pull Nomic Embed model: " +
                                e.getMessage());
                        performValidation();
                    }, ModalityState.any());
                } finally {
                    validationInProgress = false;
                }
            }
        }.queue();
    }

    private void handleChromaDBAction(@NotNull ValidationActionType action) {
        switch (action) {
            // PULL_CHROMA_DB is handled in actionPerformed (it needs the source button to
            // render inline progress). Anything reaching this method without that context
            // falls back to the no-UI variant.
            case PULL_CHROMA_DB -> pullChromaDBImageFallback();
            case START_CHROMA_DB -> startChromaDB();
            default -> NotificationUtil.sendNotification(project,
                    "Unknown action requested for ChromaDB");
        }
    }

    /**
     * Pull the ChromaDB image, replacing the source button with an inline indeterminate
     * progress bar + status label so the user has immediate, visible feedback inside the
     * RAG settings panel (the IDE's background-task spinner alone is too easy to miss).
     * The widgets are discarded on completion when {@link #performValidation()} rebuilds
     * the validator panel from scratch.
     */
    private void pullChromaDBImage(@NotNull JButton sourceButton) {
        Container parent = sourceButton.getParent();
        if (parent == null) {
            pullChromaDBImageFallback();
            return;
        }
        int buttonIndex = -1;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == sourceButton) {
                buttonIndex = i;
                break;
            }
        }
        if (buttonIndex < 0) {
            pullChromaDBImageFallback();
            return;
        }

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new java.awt.Dimension(120, sourceButton.getPreferredSize().height));
        JLabel statusLabel = new JLabel("Starting pull…");

        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.X_AXIS));
        progressPanel.add(statusLabel);
        progressPanel.add(Box.createHorizontalStrut(8));
        progressPanel.add(bar);

        parent.remove(buttonIndex);
        parent.add(progressPanel, buttonIndex);
        parent.revalidate();
        parent.repaint();

        // Why we bypass chromaDBManager.pullChromaDockerImage here: that path wraps the work
        // in ProgressManager.run(new Task.Backgroundable(...)), whose BackgroundableProcessIndicator
        // setup needs an EDT pass — and the IntelliJ Settings dialog holds modal context, so
        // that EDT work is deferred until Apply/OK is pressed. Net result: clicking "Pull
        // Image" inside Settings looked frozen until the user pressed Apply.
        //
        // The inline widget below is our visible feedback, so we don't need the status-bar
        // Backgroundable indicator at all. Direct pooled-thread submission + ModalityState.any()
        // for the EDT updates sidesteps the modal-deferral entirely.
        ChromaDockerService dockerService = ApplicationManager.getApplication()
                .getService(ChromaDockerService.class);
        java.util.function.Consumer<String> progressListener = status ->
                ApplicationManager.getApplication().invokeLater(
                        () -> statusLabel.setText(status), ModalityState.any());

        ChromaDBStatusCallback callback = new ChromaDBStatusCallback() {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.sendNotification(project, "ChromaDB image pulled successfully");
                    performValidation();
                }, ModalityState.any());
            }

            @Override
            public void onError(String message) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.sendNotification(project, "Failed to pull ChromaDB image: " + message);
                    performValidation();
                }, ModalityState.any());
            }
        };

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                dockerService.pullChromaDockerImage(callback, null, progressListener);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    /** Fallback when the source button isn't laid out where we expect — just delegate to the
     *  service and rely on the background-task spinner + completion notification. */
    private void pullChromaDBImageFallback() {
        NotificationUtil.sendNotification(project,
                "Pulling ChromaDB image — see IDE status bar for progress.");
        chromaDBManager.pullChromaDockerImage(project, new ChromaDBStatusCallback() {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.sendNotification(project, "ChromaDB image pulled successfully");
                    performValidation();
                }, ModalityState.any());
            }

            @Override
            public void onError(String message) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.sendNotification(project, "Failed to pull ChromaDB image: " + message);
                    performValidation();
                }, ModalityState.any());
            }
        });
    }

    private void startChromaDB() {
        // Immediate confirmation balloon — the actual work runs under a background Task whose
        // progress only shows in the status-bar spinner, which is easy to miss. Without this,
        // a first-time pull (~500 MB) makes the button look broken for several minutes.
        NotificationUtil.sendNotification(project,
                "Starting ChromaDB — first run pulls the Docker image (may take a few minutes). " +
                        "Watch the IDE status bar for progress.");
        chromaDBManager.startChromaDB(project, new ChromaDBStatusCallback() {
            @Override
            public void onSuccess() {
                refreshValidationState("ChromaDB started successfully");
            }

            @Override
            public void onError(String message) {
                refreshValidationState("Failed to start ChromaDB: " + message);
            }
        });
    }

    private void refreshValidationState(String notification) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                NotificationUtil.sendNotification(project, notification);
                performValidation();
                settingsComponent.updateValidationStatus();
            } finally {
                validationInProgress = false; // Reset flag
            }
        }, ModalityState.any());
    }
}