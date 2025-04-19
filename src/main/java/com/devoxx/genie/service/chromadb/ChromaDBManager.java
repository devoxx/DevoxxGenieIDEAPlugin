package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import com.devoxx.genie.service.rag.validator.ChromeDBValidator;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.devoxx.genie.util.HttpClientProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public final class ChromaDBManager {

    private static Project theProject;
    private ChromaDBService service;

    @NotNull
    public static ChromaDBManager getInstance(Project project) {
        theProject = project;
        return ApplicationManager.getApplication().getService(ChromaDBManager.class);
    }

    public ChromaDBManager() {
        ApplicationManager.getApplication().invokeLater(this::initChromaDBService);
    }

    private void initChromaDBService() {
        String url = "http://localhost:" + DevoxxGenieStateService.getInstance().getIndexerPort();
        OkHttpClient client = HttpClientProvider.getClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ChromaDBService.class);
    }

    public @NotNull List<ChromaCollection> listCollections() throws IOException {
        if (service != null) {
            retrofit2.Response<ChromaCollection[]> execute = service.getCollections().execute();
            if (execute.isSuccessful() && execute.body() != null) {
                return new ArrayList<>(List.of(execute.body()));
            } else {
                throw new IOException("Failed to list collections");
            }
        } else {
            NotificationUtil.sendNotification(ChromaDBManager.theProject, "ChromeDB service not available");
            return List.of();
        }
    }

    public void deleteCollection(String collectionId) throws IOException {
        service.deleteCollection(collectionId).execute();
    }

    public int countDocuments(String collectionName) throws IOException {
        retrofit2.Response<Integer> execute = service.getCount(collectionName).execute();
        if (execute.isSuccessful() && execute.body() != null) {
            return execute.body();
        } else {
            throw new IOException("Failed to count documents");
        }
    }

    public void startChromaDB(Project project, ChromaDBStatusCallback callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Start ChromaDB") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                startContainer(project, indicator, callback);
            }
        });
    }

    private void startContainer(Project project,
                                @NotNull ProgressIndicator indicator,
                                ChromaDBStatusCallback callback) {
        indicator.setText("Starting ChromaDB container...");
        try {
            ChromaDockerService dockerService = ApplicationManager.getApplication()
                    .getService(ChromaDockerService.class);
            dockerService.startChromaDB(project, new ChromaDBStatusCallback() {
                @Override
                public void onSuccess() {
                    // Wait for ChromaDB to be fully operational
                    indicator.setText("Waiting for ChromaDB to be ready...");
                    if (waitForChromaDB(indicator)) {
                        callback.onSuccess();
                    } else {
                        callback.onError("ChromaDB started but not responding");
                    }
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            });
        } catch (Exception e) {
            log.error("Failed to start ChromaDB", e);
            callback.onError("Failed to start ChromaDB: " + e.getMessage());
        }
    }

    public void pullChromaDockerImage(Project project, ChromaDBStatusCallback callback) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Pulling chromaDB image") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Pulling chromaDB docker image...");

                try {
                    ChromaDockerService dockerService =
                            ApplicationManager.getApplication().getService(ChromaDockerService.class);
                    dockerService.pullChromaDockerImage(callback);
                    callback.onSuccess();
                } catch (Exception e) {
                    log.error("Error pulling ChromaDB image", e);
                    callback.onError("Error pulling ChromaDB image: " + e.getMessage());
                }
            }
        });
    }

    private boolean waitForChromaDB(@NotNull ProgressIndicator indicator) {
        ChromeDBValidator validator = new ChromeDBValidator();
        int maxAttempts = 30; // 30 seconds timeout
        int attempts = 0;

        while (attempts < maxAttempts) {
            if (indicator.isCanceled()) {
                return false;
            }

            if (validator.isValid()) {
                return true;
            }

            try {
                Thread.sleep(1000); // Wait 1 second between checks
                attempts++;
                indicator.setFraction((double) attempts / maxAttempts);
                indicator.setText(String.format("Waiting for ChromaDB to be ready... (%d/%d seconds)",
                        attempts, maxAttempts));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

}
