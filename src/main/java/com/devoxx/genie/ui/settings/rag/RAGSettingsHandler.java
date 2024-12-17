package com.devoxx.genie.ui.settings.rag;

import com.devoxx.genie.service.chromadb.ChromaDBManager;
import com.devoxx.genie.service.chromadb.ChromaDBStatusCallback;
import com.devoxx.genie.chatmodel.local.ollama.OllamaModelService;
import com.devoxx.genie.service.rag.RagValidatorService;
import com.devoxx.genie.service.rag.validator.ValidationActionType;
import com.devoxx.genie.service.rag.validator.ValidationResult;
import com.devoxx.genie.service.rag.validator.ValidatorStatus;
import com.devoxx.genie.service.rag.validator.ValidatorType;
import com.devoxx.genie.ui.panel.ValidatorsPanel;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
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
        this.chromaDBManager = ChromaDBManager.getInstance();
    }

    public void performValidation() {
        if (!isInitialValidationDone) {
            isInitialValidationDone = true;
        } else if (validationInProgress) {
            validationQueued = true;
            return;
        }

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
        });
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
            handleChromaDBAction(action);
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
                    });
                } catch (IOException e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.sendNotification(project, "Failed to pull Nomic Embed model: " +
                                e.getMessage());
                        performValidation();
                    });
                } finally {
                    validationInProgress = false;
                }
            }
        }.queue();
    }

    private void handleChromaDBAction(@NotNull ValidationActionType action) {
        switch (action) {
            case PULL_CHROMA_DB -> pullChromaDBImage();
            case START_CHROMA_DB -> startChromaDB();
            default -> NotificationUtil.sendNotification(project,
                    "Unknown action requested for ChromaDB");
        }
    }

    private void pullChromaDBImage() {
        new Task.Backgroundable(project, "Pulling ChromaDB Image") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                chromaDBManager.pullChromaDockerImage(project, new ChromaDBStatusCallback() {
                    @Override
                    public void onSuccess() {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.sendNotification(project,
                                    "ChromaDB image pulled successfully");
                            performValidation();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.sendNotification(project,
                                    "Failed to pull ChromaDB image: " + message);
                            performValidation();
                        });
                    }
                });
            }
        }.queue();
    }

    private void startChromaDB() {
        new Task.Backgroundable(project, "Starting ChromaDB") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
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
        }.queue();
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
        });
    }
}