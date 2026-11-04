package com.devoxx.genie.service.rag.manifest;

import com.devoxx.genie.service.rag.embedding.EmbeddingFingerprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.assertj.core.api.Assertions.assertThat;

class IndexManifestTest {

    private static final String NOMIC = EmbeddingFingerprint.DEFAULT.id();
    private static final String BGE = EmbeddingFingerprint.ollama("bge-m3").id();

    @Test
    void mtimeBumpWithoutContentChangeDoesNotInvalidate(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Unchanged.java");
        Files.writeString(file, "class C { void m() {} }\n");

        IndexManifest manifest = new InMemoryIndexManifest();
        manifest.markIndexed(file, 3, NOMIC);
        assertThat(manifest.isCurrent(file, NOMIC)).isTrue();

        // Bump mtime forward — simulates a `touch`, a git checkout, or an IDE save with
        // no actual edit. Pre-manifest code re-indexed every time. Content hash says no.
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 10_000));

        assertThat(manifest.isCurrent(file, NOMIC))
                .as("identical bytes + advanced mtime must NOT trigger a re-index")
                .isTrue();
    }

    @Test
    void contentEditInvalidatesEvenWhenMtimeStaysTheSame(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Edited.java");
        Files.writeString(file, "class C { void m() {} }\n");
        FileTime mtime = Files.getLastModifiedTime(file);

        IndexManifest manifest = new InMemoryIndexManifest();
        manifest.markIndexed(file, 3, NOMIC);

        Files.writeString(file, "class C { void m() { System.out.println(\"changed\"); } }\n");
        // Force mtime back to the original; the manifest must still detect the change.
        Files.setLastModifiedTime(file, mtime);

        assertThat(manifest.isCurrent(file, NOMIC))
                .as("content differs even though mtime is unchanged; content-hash check must catch it")
                .isFalse();
    }

    @Test
    void schemaVersionChangeInvalidatesAllEntries(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Old.java");
        Files.writeString(file, "class C {}\n");

        // Simulate a manifest with an entry written under a previous schema version.
        InMemoryIndexManifest manifest = new InMemoryIndexManifest();
        manifest.entries.put(file.toAbsolutePath().toString(),
                new IndexManifestEntry(InMemoryIndexManifest.sha1OrNull(file),
                        Files.getLastModifiedTime(file).toMillis(),
                        System.currentTimeMillis(),
                        1,
                        "pre-v2-mystery-schema",
                        NOMIC));

        assertThat(manifest.isCurrent(file, NOMIC))
                .as("entries written under a different embedding schema version must NOT be treated as current")
                .isFalse();
    }

    @Test
    void jsonFileManifestRoundTripsThroughDisk(@TempDir Path tmp) throws IOException {
        Path storage = tmp.resolve("manifest.json");
        Path file = tmp.resolve("Roundtrip.java");
        Files.writeString(file, "class R {}\n");

        JsonFileIndexManifest writer = new JsonFileIndexManifest(storage);
        writer.markIndexed(file, 7, NOMIC);
        writer.flush();

        assertThat(Files.exists(storage)).as("flush must create the manifest file").isTrue();
        assertThat(Files.size(storage)).isGreaterThan(0);

        // Reload in a fresh instance — proves the on-disk format works.
        JsonFileIndexManifest reader = new JsonFileIndexManifest(storage);
        assertThat(reader.isCurrent(file, NOMIC))
                .as("reloaded manifest must recognise the indexed file")
                .isTrue();
    }

    @Test
    void entryIsOnlyCurrentForTheFingerprintThatEmbeddedIt(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Model.java");
        Files.writeString(file, "class M {}\n");

        IndexManifest manifest = new InMemoryIndexManifest();
        manifest.markIndexed(file, 2, BGE);

        assertThat(manifest.isCurrent(file, BGE)).isTrue();
        assertThat(manifest.isCurrent(file, NOMIC))
                .as("vectors from another embedding model are not comparable, so the file must be re-embedded")
                .isFalse();
    }

    @Test
    void legacyEntryWithoutFingerprintCountsAsNomicEmbedText(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Legacy.java");
        Files.writeString(file, "class L {}\n");

        InMemoryIndexManifest manifest = new InMemoryIndexManifest();
        manifest.entries.put(file.toAbsolutePath().toString(),
                new IndexManifestEntry(InMemoryIndexManifest.sha1OrNull(file), 0L, 0L, 1,
                        "v2", null));

        assertThat(manifest.isCurrent(file, NOMIC))
                .as("pre-TASK-212 entries were all built with nomic-embed-text; upgrading must not force a re-index")
                .isTrue();
        assertThat(manifest.isCurrent(file, BGE)).isFalse();
        assertThat(manifest.indexFingerprint()).contains(NOMIC);
    }

    @Test
    void indexFingerprintIsEmptyUntilSomethingIsIndexed(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("First.java");
        Files.writeString(file, "class F {}\n");

        IndexManifest manifest = new InMemoryIndexManifest();
        assertThat(manifest.indexFingerprint()).isEmpty();

        manifest.markIndexed(file, 1, BGE);
        assertThat(manifest.indexFingerprint()).contains(BGE);

        manifest.markRemoved(file);
        assertThat(manifest.indexFingerprint()).isEmpty();
    }

    @Test
    void fingerprintSurvivesTheDiskRoundTrip(@TempDir Path tmp) throws IOException {
        Path storage = tmp.resolve("manifest.json");
        Path file = tmp.resolve("Disk.java");
        Files.writeString(file, "class D {}\n");

        JsonFileIndexManifest writer = new JsonFileIndexManifest(storage);
        writer.markIndexed(file, 1, BGE);
        writer.flush();

        JsonFileIndexManifest reader = new JsonFileIndexManifest(storage);
        assertThat(reader.isCurrent(file, BGE)).isTrue();
        assertThat(reader.indexFingerprint()).contains(BGE);
    }

    @Test
    void manifestWrittenBeforeFingerprintsStillLoads(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Old.java");
        Files.writeString(file, "class O {}\n");
        Path storage = tmp.resolve("manifest.json");
        // Exact shape of a pre-TASK-212 sidecar: no embeddingFingerprint field.
        Files.writeString(storage, """
                { "%s" : { "contentHash" : "%s", "lastModified" : 1, "indexedAt" : 2,
                           "segmentCount" : 3, "schemaVersion" : "v2" } }
                """.formatted(file.toAbsolutePath().toString().replace("\\", "\\\\"),
                InMemoryIndexManifest.sha1OrNull(file)));

        JsonFileIndexManifest manifest = new JsonFileIndexManifest(storage);

        assertThat(manifest.isCurrent(file, NOMIC)).isTrue();
        assertThat(manifest.indexFingerprint()).contains(NOMIC);
    }

    @Test
    void clearDropsEveryEntry(@TempDir Path tmp) throws IOException {
        Path a = tmp.resolve("A.java");
        Path b = tmp.resolve("B.java");
        Files.writeString(a, "class A {}\n");
        Files.writeString(b, "class B {}\n");
        IndexManifest manifest = new InMemoryIndexManifest();
        manifest.markIndexed(a, 1, NOMIC);
        manifest.markIndexed(b, 1, NOMIC);

        manifest.clear();

        assertThat(manifest.trackedPaths()).isEmpty();
        assertThat(manifest.indexFingerprint()).isEmpty();
    }

    @Test
    void flushIsAtomicAndIdempotent(@TempDir Path tmp) throws IOException {
        Path storage = tmp.resolve("manifest.json");
        Path file = tmp.resolve("F.java");
        Files.writeString(file, "class F {}\n");

        JsonFileIndexManifest manifest = new JsonFileIndexManifest(storage);
        manifest.markIndexed(file, 1, NOMIC);
        manifest.flush();
        long firstSize = Files.size(storage);

        // No mutations between flushes — second flush should be a no-op (no tmp file left behind).
        manifest.flush();
        assertThat(Files.size(storage)).isEqualTo(firstSize);
        assertThat(Files.exists(storage.resolveSibling("manifest.json.tmp"))).isFalse();
    }
}
