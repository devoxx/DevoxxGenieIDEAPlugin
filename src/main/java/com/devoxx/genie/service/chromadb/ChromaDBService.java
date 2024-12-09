package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ChromaDBService {
    @GET("/api/v1/collections")
    Call<ChromaCollection[]> getCollections();

    @GET("/api/v1/collections/{collectionName}/count")
    Call<Integer> getCount(@Path("collectionName") String collectionName);

    @DELETE("/api/v1/collections/{collectionName}")
    Call<Void> deleteCollection(@Path("collectionName") String collectionName);
}
