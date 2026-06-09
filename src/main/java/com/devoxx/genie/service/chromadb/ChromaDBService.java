package com.devoxx.genie.service.chromadb;

import com.devoxx.genie.service.chromadb.model.ChromaCollection;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ChromaDBService {
    @GET("/api/v2/tenants/default_tenant/databases/default_database/collections")
    Call<ChromaCollection[]> getCollections();

    @GET("/api/v2/tenants/default_tenant/databases/default_database/collections/{collectionId}/count")
    Call<Integer> getCount(@Path("collectionId") String collectionId);

    @DELETE("/api/v2/tenants/default_tenant/databases/default_database/collections/{collectionName}")
    Call<Void> deleteCollection(@Path("collectionName") String collectionName);
}
