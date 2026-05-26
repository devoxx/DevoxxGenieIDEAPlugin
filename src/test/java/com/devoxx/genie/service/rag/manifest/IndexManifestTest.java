package com.devoxx.genie.service.rag.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.assertj.core.api.Assertions.assertThat;

class IndexManifestTest {

    @Test
    void mtimeBumpWithoutContentChangeDoesNotInvalidate(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Unchanged.java");
        Files.writeString(file, "class C { void m() {} }\n");

        IndexManifest manifest = new InMemoryIndexManifest();
        manifest.markIndexed(file, 3);
        assertThat(manifest.isCurrent(file)).isTrue();

        // Bump mtime forward — simulates a `touch`, a git checkout, or an IDE save with
        // no actual edit. Pre-manifest code re-indexed every time. Content hash says no.
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 10_000));

        assertThat(manifest.isCurrent(file))
                .as("identical bytes + advanced mtime must NOT trigger a re-index")
                .isTrue();
    }

    @Test
    void contentEditInvalidatesEvenWhenMtimeStaysTheSame(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("Edited.java");
        Files.writeString(file, "class C { void m() {} }\n");
        FileTime mtime = Files.getLastModifiedTime(file);

        IndexManifest manifest = new InMemoryIndexManifest();
        manifest.markIndexed(file, 3);

        Files.writeString(file, "class C { void m() { System.out.println(\"changed\"); } }\n");
        // Force mtime back to the original; the manifest must still detect the change.
        Files.setLastModifiedTime(file, mtime);

        assertThat(manifest.isCurrent(file))
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
                        "pre-v2-mystery-schema"));

        assertThat(manifest.isCurrent(file))
                .as("entries written under a different embedding schema version must NOT be treated as current")
                .isFalse();
    }

    @Test
    void jsonFileManifestRoundTripsThroughDisk(@TempDir Path tmp) throws IOException {
        Path storage = tmp.resolve("manifest.json");
        Path file = tmp.resolve("Roundtrip.java");
        Files.writeString(file, "class R {}\n");

        JsonFileIndexManifest writer = new JsonFileIndexManifest(storage);
        writer.markIndexed(file, 7);
        writer.flush();

        assertThat(Files.exists(storage)).as("flush must create the manifest file").isTrue();
        assertThat(Files.size(storage)).isGreaterThan(0);

        // Reload in a fresh instance — proves the on-disk format works.
        JsonFileIndexManifest reader = new JsonFileIndexManifest(storage);
        assertThat(reader.isCurrent(file))
                .as("reloaded manifest must recognise the indexed file")
                .isTrue();
    }

    @Test
    void flushIsAtomicAndIdempotent(@TempDir Path tmp) throws IOException {
        Path storage = tmp.resolve("manifest.json");
        Path file = tmp.resolve("F.java");
        Files.writeString(file, "class F {}\n");

        JsonFileIndexManifest manifest = new JsonFileIndexManifest(storage);
        manifest.markIndexed(file, 1);
        manifest.flush();
        long firstSize = Files.size(storage);

        // No mutations between flushes — second flush should be a no-op (no tmp file left behind).
        manifest.flush();
        assertThat(Files.size(storage)).isEqualTo(firstSize);
        assertThat(Files.exists(storage.resolveSibling("manifest.json.tmp"))).isFalse();
    }
}
