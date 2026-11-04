package com.devoxx.genie.service.rag.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.Strictness;
import java.io.StringReader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Standalone diagnostic CLI for the project's RAG store. Talks to the same ChromaDB and Ollama
 * the plugin uses, but bypasses the IntelliJ service layer so it can run from {@code gradle
 * ragQuery}.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code query <collection> <text> [topN=10]} — embed and search; prints rank/score/path/snippet</li>
 *   <li>{@code list <collection> [substringFilter]} — print distinct file paths in a collection</li>
 * </ul>
 *
 * <p>Env overrides: {@code DEVOXXGENIE_CHROMA_URL} (default {@code http://localhost:8000}),
 * {@code DEVOXXGENIE_OLLAMA_URL} (default {@code http://localhost:11434}),
 * {@code DEVOXXGENIE_EMBED_MODEL} (default {@code nomic-embed-text}).
 */
public final class RagCli {

    private static final String DEFAULT_CHROMA_URL = "http://localhost:8000";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_EMBED_MODEL = "nomic-embed-text";

    private RagCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { printUsage(); System.exit(2); }
        String cmd = args[0].toLowerCase(Locale.ROOT);
        switch (cmd) {
            case "query" -> runQuery(args);
            case "list"  -> runList(args);
            default -> { printUsage(); System.exit(2); }
        }
    }

    // ---------------- query ----------------

    private static void runQuery(String[] args) {
        if (args.length < 3) { printUsage(); System.exit(2); }
        String collection = args[1];
        String text = args[2];
        int topN = args.length >= 4 ? Integer.parseInt(args[3]) : 10;

        EmbeddingModel embedder = OllamaEmbeddingModel.builder()
                .baseUrl(env("DEVOXXGENIE_OLLAMA_URL", DEFAULT_OLLAMA_URL))
                .modelName(env("DEVOXXGENIE_EMBED_MODEL", DEFAULT_EMBED_MODEL))
                .build();

        ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                .baseUrl(env("DEVOXXGENIE_CHROMA_URL", DEFAULT_CHROMA_URL))
                .collectionName(collection)
                .build();

        Embedding q = embedder.embed(text).content();
        var results = store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(q)
                .maxResults(topN)
                .build()).matches();

        System.out.printf("Query: %s%nCollection: %s%nTop %d results:%n%n", text, collection, results.size());
        int rank = 0;
        for (var m : results) {
            rank++;
            String path = m.embedded().metadata().getString("filePath");
            String snippet = oneLine(m.embedded().text(), 200);
            System.out.printf("[%2d] score=%.4f  %s%n     %s%n%n",
                    rank, m.score(), path != null ? path : "(unknown)", snippet);
        }
    }

    // ---------------- list ----------------

    private static void runList(String[] args) throws Exception {
        if (args.length < 2) { printUsage(); System.exit(2); }
        String collection = args[1];
        String filter = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : null;

        String chroma = env("DEVOXXGENIE_CHROMA_URL", DEFAULT_CHROMA_URL);
        // ChromaDB's HTTP server rejects HTTP/2 with "Invalid HTTP request received"; pin to 1.1.
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        // Resolve collection name -> UUID (Chroma v1 API).
        HttpResponse<String> meta = http.send(
                HttpRequest.newBuilder(URI.create(chroma + "/api/v1/collections/" + collection)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (meta.statusCode() != 200) {
            System.err.println("Collection not found: " + collection + " (HTTP " + meta.statusCode() + ")");
            System.err.println("Body: " + meta.body());
            System.exit(1);
        }
        JsonElement metaEl = parseLenient(meta.body());
        if (!metaEl.isJsonObject() || metaEl.getAsJsonObject().get("id") == null) {
            System.err.println("Unexpected metadata response for collection " + collection + ":");
            System.err.println(meta.body());
            System.exit(1);
        }
        String uuid = metaEl.getAsJsonObject().get("id").getAsString();

        // Page through entries; print unique filePaths.
        int limit = 1000;
        int offset = 0;
        Set<String> unique = new LinkedHashSet<>();
        while (true) {
            String body = "{\"limit\":" + limit + ",\"offset\":" + offset + ",\"include\":[\"metadatas\"]}";
            HttpResponse<String> resp = http.send(
                    HttpRequest.newBuilder(URI.create(chroma + "/api/v1/collections/" + uuid + "/get"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonElement respEl = parseLenient(resp.body());
            if (!respEl.isJsonObject()) {
                System.err.println("Unexpected /get response (HTTP " + resp.statusCode() + "):");
                System.err.println(resp.body().length() > 500 ? resp.body().substring(0, 500) + "…" : resp.body());
                System.exit(1);
            }
            JsonObject obj = respEl.getAsJsonObject();
            JsonElement metasEl = obj.get("metadatas");
            if (metasEl == null || metasEl.isJsonNull()) break;
            JsonArray metas = metasEl.getAsJsonArray();
            if (metas.isEmpty()) break;
            for (JsonElement el : metas) {
                if (el == null || el.isJsonNull()) continue;
                JsonElement fp = el.getAsJsonObject().get("filePath");
                if (fp == null || fp.isJsonNull()) continue;
                String path = fp.getAsString();
                if (filter == null || path.toLowerCase(Locale.ROOT).contains(filter)) {
                    unique.add(path);
                }
            }
            if (metas.size() < limit) break;
            offset += limit;
        }
        unique.forEach(System.out::println);
        System.err.printf("%n%d unique file%s%n", unique.size(), unique.size() == 1 ? "" : "s");
    }

    // ---------------- helpers ----------------

    private static JsonElement parseLenient(String body) {
        JsonReader r = new JsonReader(new StringReader(body));
        r.setStrictness(Strictness.LENIENT);
        return JsonParser.parseReader(r);
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static String oneLine(String s, int max) {
        if (s == null) return "";
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }

    private static void printUsage() {
        System.err.println("""
                Usage:
                  ragQuery query <collection> <text> [topN=10]
                  ragQuery list  <collection> [substring]

                Env:
                  DEVOXXGENIE_CHROMA_URL   (default http://localhost:8000)
                  DEVOXXGENIE_OLLAMA_URL   (default http://localhost:11434)
                  DEVOXXGENIE_EMBED_MODEL  (default nomic-embed-text)
                """);
    }
}
