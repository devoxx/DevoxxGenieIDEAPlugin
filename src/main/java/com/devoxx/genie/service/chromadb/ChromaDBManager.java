package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
public final class ChromaDBManager {

    private final ChromaDBService service;

    @NotNull
    public static ChromaDBManager getInstance() {
        return ApplicationManager.getApplication().getService(ChromaDBManager.class);
    }

    public ChromaDBManager() {

        String url = "http://localhost:" + DevoxxGenieStateService.getInstance().getIndexerPort();

        OkHttpClient client = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(ChromaDBService.class);
    }

    public @NotNull List<ChromaCollection> listCollections() throws IOException {
        retrofit2.Response<ChromaCollection[]> execute = service.getCollections().execute();
        if (execute.isSuccessful() && execute.body() != null) {
            return new ArrayList<>(List.of(execute.body()));
        } else {
            throw new IOException("Failed to list collections");
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

    public boolean isReady() {
        try {
            listCollections();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}