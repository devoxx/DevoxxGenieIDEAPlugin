# RAG retrieval benchmark

Measures how well an embedding configuration finds the right files for natural-language
questions about this repository. Use it to compare embedding models, query/document prefixes
and truncated dimensions before changing RAG defaults.

It runs outside the IDE and **never touches ChromaDB**:
- It indexes the repository into an in-memory store using the plugin's own chunker
  (`RagChunker`), so the chunks are the same as in the plugin.
- It asks every question in the dataset and scores where the expected file ranks.

Only Ollama is needed.

## Running it

```bash
export JAVA_HOME=~/.sdkman/candidates/java/21-zulu

# Baseline: nomic-embed-text, no prefixes (the plugin's current setup)
./gradlew ragBenchmark --args="--out build/rag-benchmark/nomic-no-prefix.json"

# nomic's recommended prefixes
./gradlew ragBenchmark --args="--prefix-preset nomic --out build/rag-benchmark/nomic-prefixed.json"

# Another model, vectors truncated to 256 dimensions
./gradlew ragBenchmark --args="--model bge-m3 --dimension 256"

# Custom prefixes (environment variables keep trailing spaces without quoting tricks)
DEVOXXGENIE_EMBED_QUERY_PREFIX="Query: " DEVOXXGENIE_EMBED_DOC_PREFIX="Document: " ./gradlew ragBenchmark

# All options
./gradlew ragBenchmark --args="--help"
```

The model must be pulled in Ollama first (`ollama pull nomic-embed-text`). A full run on this
repository embeds every indexable file and takes a few minutes. The same command is available as
`./gradlew ragQuery --args="benchmark …"`.

| Option | Environment variable | Default |
| --- | --- | --- |
| `--model` | `DEVOXXGENIE_EMBED_MODEL` | `nomic-embed-text` |
| `--ollama-url` | `DEVOXXGENIE_OLLAMA_URL` | `http://localhost:11434` |
| `--query-prefix` | `DEVOXXGENIE_EMBED_QUERY_PREFIX` | none |
| `--doc-prefix` | `DEVOXXGENIE_EMBED_DOC_PREFIX` | none |
| `--prefix-preset` (`none`, `nomic`) | | none; explicit prefixes win |
| `--dimension` | `DEVOXXGENIE_EMBED_DIMENSION` | model's native size |
| `--top-chunks` | | 50 |
| `--dataset` | | `src/test/resources/rag-benchmark/devoxxgenie-questions.json` |
| `--repo` | | `.` |
| `--out` | | none (console only) |

Gradle splits `--args` on spaces, so a prefix that ends in a space is easiest to pass through
its environment variable (see above). For nomic, use the preset.

## Which files are indexed

`git ls-files` in `--repo`, filtered with the plugin's factory-default included extensions and
excluded directories (`ProjectScanDefaults`), minus the dataset's `excludePaths`. Using
`git ls-files` approximates the plugin's default `.gitignore` handling. Your own project-scan
settings in the IDE are not applied.

`git ls-files` only lists **tracked** files: new files aren't indexed until they're added with
`git add`, while uncommitted edits to tracked files are indexed as they are on disk. So the
corpus depends on the working tree; compare runs made on the same commit with the same
tracked files.

## Reading the output

Excerpt from a real run (nomic-embed-text with `--prefix-preset nomic`, 2026-11-03):

```
pass rank  question                                 best retrieved file (on miss)
✓    3     agent-tool-call-limit
✗    11    parallel-sub-agents                      docusaurus/docs/features/agent-mode.md
✗    -     mid-task-steering                        CHANGELOG.md
✓    1     rag-query-paraphrase-fusion
...
Questions: 28   files: 1414 (skipped 0)   chunks: 18657 (failed 0)
recall@1 0.250   recall@5 0.643   recall@10 0.821   MRR 0.425
Index 140.4s   queries 0.8s
```

- **rank**: position of the first expected file among the retrieved *files*. Chunks are
  collapsed to files first, so three chunks from one file count as one entry. `-` means no
  expected file was in the top `--top-chunks` chunks.
- **pass**: an expected file is in the top 5.
- **recall@k**: fraction of questions with an expected file in the top *k* files. It's a hit
  rate: more expected files don't make a question easier to hit twice.
- **MRR**: mean of 1/rank over all questions, where a miss counts 0. It rewards ranking the
  answer first rather than just somewhere in the top 10.
- Retrieval uses **no minimum score**, unlike the plugin (default 0.7), so a threshold can't
  hide differences in ranking between configurations.

`--out` writes the same results as JSON, adding the run's model, URL, timestamp and git commit
plus each question's top 10 files, so runs can be compared over time. Compare runs on the same
commit: the corpus changes with the code.

## Adding or changing questions

Edit `devoxxgenie-questions.json`. Each question has:

```json
{
  "id": "rag-reindex-on-save",
  "question": "When I save a file, how does the semantic index get updated automatically?",
  "expected": ["src/main/java/com/devoxx/genie/service/rag/watcher/RAGFileWatcher.java"]
}
```

- Ask as a developer or user would, without class names they wouldn't know. Otherwise the
  benchmark measures keyword overlap rather than semantic retrieval.
- List every file that genuinely answers the question (a class and its docs page, say). A hit
  on any of them counts.
- Paths are repository-relative and must exist. `BenchmarkDatasetTest` fails when a file moves
  or can't be part of the corpus, so update the dataset together with the refactoring.
- Bump `version` when you change questions in a way that makes old scores incomparable.

`excludePaths` keeps test fixtures that are copies of real source files (for example
`src/test/resources/rag-chunking/`) out of the corpus, so they don't compete with the originals.
