package com.devoxx.genie.service.agent.team;

import com.devoxx.genie.chatmodel.agentteam.AgentTeamChatModelFactory;
import com.devoxx.genie.model.agent.AgentDefinition;
import com.devoxx.genie.model.agent.AgentResult;
import com.devoxx.genie.model.enumarations.ModelProvider;
import com.devoxx.genie.service.LLMProviderService;
import com.devoxx.genie.service.agent.AgentApprovalService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DockerUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs one delegated Agent Team task in a Docker container spawned directly via
 * docker-java with the PROJECT BIND-MOUNTED (TASK-251, Phase B of per-agent isolation).
 * <p>
 * Unlike the DockerAgents orchestrator-api backend (fresh clone, results as a branch),
 * this preserves in-editor semantics: read-only agents get the project mounted {@code ro},
 * writable agents {@code rw} — so their edits appear in the open project — while the
 * process itself is sandboxed (dropped capabilities, no-new-privileges, mem/cpu/pids
 * caps mirroring DockerAgents' DEFAULT_PROFILE).
 * <p>
 * The persona travels as a Genie-format YAML (TASK-247 mapper) mounted at {@code /agents};
 * the DockerAgents runner image executes it and writes the standard {@code result.json},
 * which is read back from the artifacts mount. Because a read-write mount bypasses the
 * per-call IDE approval dialog, spawning a writable container is itself approval-gated,
 * and the summary is annotated with {@code git status} output so the diff is reviewable.
 */
@Slf4j
public class LocalContainerAgentRunner {

    static final String REPO_MOUNT = "/session/repo";
    static final String AGENTS_MOUNT = "/agents";
    static final String ARTIFACTS_MOUNT = "/artifacts";

    // DockerAgents DEFAULT_PROFILE equivalents
    private static final long MEM_LIMIT_BYTES = 2L * 1024 * 1024 * 1024;
    private static final long NANO_CPUS = 2_000_000_000L;
    private static final long PIDS_LIMIT = 512;

    private final Project project;
    private final AgentDefinition definition;
    private final @Nullable String intent;
    private final AtomicBoolean cancelled;

    private volatile @Nullable DockerClient dockerClient;
    private volatile @Nullable String containerId;

    public LocalContainerAgentRunner(@NotNull Project project,
                                     @NotNull AgentDefinition definition,
                                     @Nullable String intent,
                                     @NotNull AtomicBoolean cancelled) {
        this.project = project;
        this.definition = definition;
        this.intent = intent;
        this.cancelled = cancelled;
    }

    /** Never throws — every failure mode becomes a readable {@link AgentResult}. */
    public @NotNull AgentResult execute(@NotNull String task) {
        long start = System.currentTimeMillis();
        String agentName = definition.getName();

        if (cancelled.get()) {
            return AgentResult.cancelled(agentName, intent);
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            return AgentResult.error(agentName, intent,
                    "Agent '" + agentName + "': the project has no base path to mount.",
                    0, elapsed(start), "container", null);
        }

        boolean readWrite = !definition.isReadOnly();
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();

        // A rw bind mount bypasses per-call approval, so gate the SPAWN like a write tool.
        if (readWrite && Boolean.TRUE.equals(settings.getAgentWriteApprovalRequired())) {
            boolean approved = AgentApprovalService.requestApproval(project,
                    "run_local_container:" + agentName,
                    "Read-write container on this project. Task: " + truncate(task, 300));
            if (!approved) {
                return AgentResult.error(agentName, intent,
                        "Agent '" + agentName + "': the user denied spawning a read-write container.",
                        0, elapsed(start), "container", null);
            }
        }

        String image = settings.getAgentTeamLocalContainerImage();
        if (image == null || image.isBlank()) {
            image = "geniebuilder-agent-runner:latest";
        }
        String sessionId = agentName + "-" + UUID.randomUUID().toString().substring(0, 8);
        int timeoutSeconds = definition.getTimeoutSeconds() != null && definition.getTimeoutSeconds() > 0
                ? definition.getTimeoutSeconds()
                : (settings.getSubAgentTimeoutSeconds() != null ? settings.getSubAgentTimeoutSeconds() : 120);

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("devoxxgenie-agent-" + agentName);
            Path agentsDir = Files.createDirectory(workDir.resolve("agents"));
            Path artifactsDir = Files.createDirectory(workDir.resolve("artifacts"));
            Files.writeString(agentsDir.resolve(agentName + ".yml"),
                    GenieAgentSpecMapper.toYaml(withResolvedBinding(definition)), StandardCharsets.UTF_8);

            DockerClient client = DockerUtil.getDockerClient();
            dockerClient = client;

            HostConfig hostConfig = new HostConfig()
                    .withBinds(
                            new Bind(basePath, new Volume(REPO_MOUNT),
                                    readWrite ? AccessMode.rw : AccessMode.ro),
                            new Bind(agentsDir.toString(), new Volume(AGENTS_MOUNT), AccessMode.ro),
                            new Bind(artifactsDir.toString(), new Volume(ARTIFACTS_MOUNT), AccessMode.rw))
                    .withMemory(MEM_LIMIT_BYTES)
                    .withNanoCPUs(NANO_CPUS)
                    .withPidsLimit(PIDS_LIMIT)
                    .withCapDrop(Capability.ALL)
                    .withSecurityOpts(List.of("no-new-privileges"))
                    // Lets the container reach local providers (Ollama, LMStudio) on Linux too
                    .withExtraHosts("host.docker.internal:host-gateway");

            String id;
            try (var createCmd = client.createContainerCmd(image)
                    .withEnv(buildEnv(agentName, task, sessionId, timeoutSeconds))
                    .withWorkingDir(REPO_MOUNT)
                    .withLabels(java.util.Map.of("devoxxgenie.agent", agentName,
                            "devoxxgenie.session", sessionId))
                    .withHostConfig(hostConfig)) {
                id = createCmd.exec().getId();
            }
            containerId = id;

            if (cancelled.get()) {
                removeContainerQuietly();
                return AgentResult.cancelled(agentName, intent);
            }
            client.startContainerCmd(id).exec();
            log.info("Local container agent '{}' started (container {}, {} mount)",
                    agentName, id.substring(0, 12), readWrite ? "rw" : "ro");

            Integer statusCode;
            try (WaitContainerResultCallback wait = client.waitContainerCmd(id).exec(new WaitContainerResultCallback())) {
                // The runner enforces MAX_SESSION_SECONDS itself; the margin covers image start-up.
                statusCode = wait.awaitStatusCode(timeoutSeconds + 30L, TimeUnit.SECONDS);
            } catch (Exception e) {
                removeContainerQuietly();
                if (cancelled.get()) {
                    return AgentResult.cancelled(agentName, intent);
                }
                return AgentResult.timeout(agentName, intent, timeoutSeconds);
            }

            AgentResult result = readResult(artifactsDir, sessionId, agentName, statusCode, start);
            if (readWrite && result.status() == AgentResult.Status.OK) {
                result = annotateWorkspaceChanges(result, basePath, start);
                refreshProjectVfs(basePath);
            }
            return result;
        } catch (com.github.dockerjava.api.exception.NotFoundException e) {
            return AgentResult.error(agentName, intent,
                    "Agent '" + agentName + "': runner image '" + image + "' not found. Build it with "
                            + "DockerAgents' bin/build.sh or configure another image in "
                            + "Settings > Agent > Agent Team.",
                    0, elapsed(start), "container", null);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return AgentResult.error(agentName, intent,
                    "Agent '" + agentName + "' local container failed: " + msg
                            + ". Is Docker running?",
                    0, elapsed(start), "container", null);
        } finally {
            removeContainerQuietly();
            deleteQuietly(workDir);
        }
    }

    /** Stops and removes the container (cancellation). Safe to call at any point. */
    public void cancel() {
        cancelled.set(true);
        removeContainerQuietly();
    }

    /**
     * The exported Genie YAML must carry a concrete provider/model — inside the container
     * there is no "conversation model" to inherit. Resolves the same binding chain the
     * in-process runner uses; on failure the binding is left empty (the runner's own
     * default applies) rather than failing the spawn.
     */
    private @NotNull AgentDefinition withResolvedBinding(@NotNull AgentDefinition def) {
        AgentDefinition copy = def.copy();
        if (copy.getModelProvider() == null || copy.getModelProvider().isBlank()) {
            try {
                var binding = AgentTeamChatModelFactory.resolveBinding(def.getName());
                copy.setModelProvider(binding.providerName());
                copy.setModelName(binding.modelName() != null ? binding.modelName() : "");
            } catch (Exception e) {
                log.warn("Could not resolve a model binding for container agent '{}': {}",
                        def.getName(), e.getMessage());
            }
        }
        return copy;
    }

    /**
     * Environment for the DockerAgents runner: identity + task + the credential/host the
     * agent's provider needs. Local provider URLs are rewritten to host.docker.internal
     * so the container can reach servers on the developer's machine. Every lookup is
     * defensive — a missing credential becomes a runner-side readable failure, never a
     * spawn-time crash.
     */
    @NotNull List<String> buildEnv(@NotNull String agentName, @NotNull String task,
                                   @NotNull String sessionId, int timeoutSeconds) {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        List<String> env = new ArrayList<>();
        env.add("AGENT_NAME=" + agentName);
        env.add("TASK_PROMPT=" + task);
        env.add("SESSION_ID=" + sessionId);
        env.add("NEEDS_REPO=0"); // the project is already mounted at /session/repo
        env.add("MAX_SESSION_SECONDS=" + timeoutSeconds);

        String provider = withResolvedBinding(definition).getModelProvider();
        if (provider == null) {
            return env;
        }
        try {
            switch (provider) {
                case "Ollama" -> env.add("OLLAMA_HOST="
                        + hostAccessibleUrl(settings.getOllamaModelUrl(), "http://host.docker.internal:11434"));
                case "LMStudio" -> env.add("LM_STUDIO_HOST="
                        + hostAccessibleUrl(settings.getLmstudioModelUrl(), "http://host.docker.internal:1234"));
                case "LLaMA", "LLaMA.c++" -> env.add("LLAMA_CPP_HOST="
                        + hostAccessibleUrl(settings.getLlamaCPPUrl(), "http://host.docker.internal:8080"));
                case "Anthropic" -> {
                    String key = LLMProviderService.getInstance().getApiKey(ModelProvider.Anthropic);
                    if (key != null && !key.isBlank()) {
                        // Same prefix routing as the DockerAgents api: OAuth tokens use a
                        // different env var than console API keys.
                        env.add((key.startsWith("sk-ant-oat") ? "ANTHROPIC_AUTH_TOKEN=" : "ANTHROPIC_API_KEY=")
                                + key.trim());
                    }
                }
                default -> log.debug("No container env mapping for provider '{}' — the runner "
                        + "will report a readable credential error if one is needed", provider);
            }
        } catch (Exception e) {
            log.warn("Skipping provider env for container agent '{}': {}", agentName, e.getMessage());
        }
        return env;
    }

    /** Rewrites localhost URLs so they resolve from inside the container. */
    static @NotNull String hostAccessibleUrl(@Nullable String url, @NotNull String fallback) {
        if (url == null || url.isBlank()) {
            return fallback;
        }
        String rewritten = url.trim()
                .replace("localhost", "host.docker.internal")
                .replace("127.0.0.1", "host.docker.internal");
        return rewritten.endsWith("/") ? rewritten.substring(0, rewritten.length() - 1) : rewritten;
    }

    private @NotNull AgentResult readResult(@NotNull Path artifactsDir, @NotNull String sessionId,
                                            @NotNull String agentName, @Nullable Integer statusCode,
                                            long start) throws IOException {
        Path resultFile = artifactsDir.resolve(sessionId).resolve("result.json");
        if (!Files.exists(resultFile)) {
            return AgentResult.error(agentName, intent,
                    "Agent '" + agentName + "': container exited (code " + statusCode
                            + ") without writing result.json — check the runner image and its logs.",
                    0, elapsed(start), "container", null);
        }
        return RemoteAgentBackend.parseResultPayload(
                Files.readString(resultFile, StandardCharsets.UTF_8),
                agentName, intent, elapsed(start), "container");
    }

    /**
     * Appends the workspace diff surface to the summary after a read-write run — the
     * container bypassed per-edit approval, so the caller (and the user) must see WHAT
     * changed and review it via Git before committing (AC #4).
     */
    private @NotNull AgentResult annotateWorkspaceChanges(@NotNull AgentResult result,
                                                          @NotNull String basePath, long start) {
        String changes = gitStatusPorcelain(basePath);
        if (changes == null) {
            return result;
        }
        String note = changes.isBlank()
                ? "\n\n(No workspace changes detected by git status.)"
                : "\n\nWorkspace changes written directly to the project (review before committing):\n"
                        + changes;
        return new AgentResult(result.agent(), result.intent(), result.status(),
                result.summary() + note, result.toolCalls(),
                elapsed(start), result.provider(), result.model());
    }

    /** {@code git status --porcelain} in the project dir; null when git is unavailable. */
    static @Nullable String gitStatusPorcelain(@NotNull String basePath) {
        try {
            Process process = new ProcessBuilder("git", "status", "--porcelain")
                    .directory(Path.of(basePath).toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return truncate(output, 2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Async recursive refresh so the editor picks up container-made changes (AC #4). */
    private static void refreshProjectVfs(@NotNull String basePath) {
        try {
            ApplicationManager.getApplication().invokeLater(() -> {
                VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath);
                if (root != null) {
                    root.refresh(true, true);
                }
            });
        } catch (Exception e) {
            log.debug("VFS refresh after container run failed", e);
        }
    }

    private void removeContainerQuietly() {
        String id = containerId;
        DockerClient client = dockerClient;
        if (id == null || client == null) {
            return;
        }
        containerId = null;
        try {
            client.removeContainerCmd(id).withForce(true).exec();
        } catch (Exception e) {
            log.debug("Failed to remove container {}: {}", id, e.getMessage());
        }
    }

    private static void deleteQuietly(@Nullable Path dir) {
        if (dir == null) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // temp dir; the OS cleans up eventually
                }
            });
        } catch (IOException e) {
            log.debug("Failed to clean temp dir {}", dir, e);
        }
    }

    private static @NotNull String truncate(@NotNull String text, int max) {
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    private static long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }
}
