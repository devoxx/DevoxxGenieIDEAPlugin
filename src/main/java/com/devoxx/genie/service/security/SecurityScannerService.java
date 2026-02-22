package com.devoxx.genie.service.security;

import com.devoxx.genie.model.security.ScannerType;
import com.devoxx.genie.model.security.SecurityFinding;
import com.devoxx.genie.model.security.SecurityScanResult;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates security scanners (gitleaks, opengrep, trivy) and creates
 * TaskSpec backlog items from findings.
 */
@Slf4j
@Service(Service.Level.PROJECT)
public final class SecurityScannerService implements Disposable {

    private final Project project;
    private volatile boolean running;
    private volatile boolean cancelled;

    public SecurityScannerService(@NotNull Project project) {
        this.project = project;
    }

    public static SecurityScannerService getInstance(@NotNull Project project) {
        return project.getService(SecurityScannerService.class);
    }

    public boolean isRunning() {
        return running;
    }

    public void cancel() {
        cancelled = true;
    }

    /**
     * Run security scan asynchronously on a pooled thread.
     *
     * @param listener progress callback (invoked on EDT)
     * @param scannerTypes which scanners to run (null = all enabled)
     */
    public void runScan(@Nullable SecurityScanListener listener,
                        @Nullable Set<ScannerType> scannerTypes) {
        if (running) {
            notifyOnEdt(listener, l -> l.onScanFailed("A scan is already running"));
            return;
        }

        running = true;
        cancelled = false;
        notifyOnEdt(listener, SecurityScanListener::onScanStarted);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SecurityScanResult result = doScan(listener, scannerTypes);
                notifyOnEdt(listener, l -> l.onScanCompleted(result));
            } catch (Exception e) {
                log.error("Security scan failed", e);
                notifyOnEdt(listener, l -> l.onScanFailed(e.getMessage()));
            } finally {
                running = false;
            }
        });
    }

    /**
     * Run security scan synchronously (for agent tool use).
     */
    public SecurityScanResult runScanSync(@Nullable Set<ScannerType> scannerTypes)
            throws SecurityScanException {
        if (running) {
            throw new SecurityScanException("A scan is already running");
        }
        running = true;
        cancelled = false;
        try {
            return doScan(null, scannerTypes);
        } finally {
            running = false;
        }
    }

    private SecurityScanResult doScan(@Nullable SecurityScanListener listener,
                                       @Nullable Set<ScannerType> scannerTypes)
            throws SecurityScanException {
        long startTime = System.currentTimeMillis();

        DevoxxGenieStateService state = DevoxxGenieStateService.getInstance();
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new SecurityScanException("Project base path not found");
        }

        List<ScannerEntry> scanners = buildScannerList(state, scannerTypes);
        if (scanners.isEmpty()) {
            throw new SecurityScanException("No scanners available. Install gitleaks, opengrep, or trivy and configure paths in Settings.");
        }

        List<SecurityFinding> allFindings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int gitleaksCount = 0;
        int opengrepCount = 0;
        int trivyCount = 0;

        for (int i = 0; i < scanners.size(); i++) {
            if (cancelled) break;

            ScannerEntry entry = scanners.get(i);
            final int idx = i;
            notifyOnEdt(listener, l -> l.onScannerStarted(
                    entry.type.getDisplayName(), idx, scanners.size()));

            try {
                List<SecurityFinding> findings = entry.scanner.scan(entry.binaryPath, basePath);
                allFindings.addAll(findings);

                switch (entry.type) {
                    case GITLEAKS -> gitleaksCount = findings.size();
                    case OPENGREP -> opengrepCount = findings.size();
                    case TRIVY -> trivyCount = findings.size();
                }

                final int count = findings.size();
                notifyOnEdt(listener, l -> l.onScannerCompleted(
                        entry.type.getDisplayName(), count));
            } catch (SecurityScanException e) {
                log.warn("Scanner {} failed: {}", entry.type.getId(), e.getMessage());
                errors.add(entry.type.getDisplayName() + ": " + e.getMessage());
                notifyOnEdt(listener, l -> l.onScannerSkipped(
                        entry.type.getDisplayName(), e.getMessage()));
            }
        }

        // Create tasks from findings (dedup against existing) — only when enabled in settings
        int tasksCreated = 0;
        if (!Boolean.FALSE.equals(state.getSecurityScanCreateSpecTasks())) {
            tasksCreated = createTasksFromFindings(allFindings);
        }
        final int finalTasksCreated = tasksCreated;
        notifyOnEdt(listener, l -> l.onTasksCreated(finalTasksCreated));

        return SecurityScanResult.builder()
                .findings(allFindings)
                .gitleaksCount(gitleaksCount)
                .opengrepCount(opengrepCount)
                .trivyCount(trivyCount)
                .durationMs(System.currentTimeMillis() - startTime)
                .errors(errors)
                .build();
    }

    private List<ScannerEntry> buildScannerList(@NotNull DevoxxGenieStateService state,
                                                  @Nullable Set<ScannerType> requested) {
        List<ScannerEntry> entries = new ArrayList<>();

        if (shouldRun(ScannerType.GITLEAKS, requested)) {
            tryResolve(ScannerType.GITLEAKS, state.getGitleaksPath(), new GitleaksScanner(), entries);
        }
        if (shouldRun(ScannerType.OPENGREP, requested)) {
            tryResolve(ScannerType.OPENGREP, state.getOpengrepPath(), new OpengrepScanner(), entries);
        }
        if (shouldRun(ScannerType.TRIVY, requested)) {
            tryResolve(ScannerType.TRIVY, state.getTrivyPath(), new TrivyScanner(), entries);
        }

        return entries;
    }

    private boolean shouldRun(ScannerType type, @Nullable Set<ScannerType> requested) {
        return requested == null || requested.contains(type);
    }

    private void tryResolve(ScannerType type, String userPath,
                             AbstractScanner scanner, List<ScannerEntry> entries) {
        try {
            String binary = SecurityBinaryManager.resolveBinary(type, userPath != null ? userPath : "");
            if (binary != null) {
                entries.add(new ScannerEntry(type, scanner, binary));
            } else {
                log.info("{} not available (binary not found)", type.getDisplayName());
            }
        } catch (SecurityScanException e) {
            log.warn("Failed to resolve {} binary: {}", type.getId(), e.getMessage());
        }
    }

    private int createTasksFromFindings(@NotNull List<SecurityFinding> findings) {
        if (findings.isEmpty()) return 0;

        SpecService specService = SpecService.getInstance(project);
        Set<String> existingTitles = specService.getAllSpecs().stream()
                .map(TaskSpec::getTitle)
                .collect(Collectors.toSet());

        int created = 0;
        for (SecurityFinding finding : findings) {
            String title = buildTaskTitle(finding);
            if (existingTitles.contains(title)) {
                continue;
            }

            try {
                TaskSpec spec = TaskSpec.builder()
                        .title(title)
                        .status("To Do")
                        .priority(finding.getSeverity())
                        .labels(buildLabels(finding))
                        .description(finding.getDescription())
                        .build();

                specService.createTask(spec);
                existingTitles.add(title);
                created++;
            } catch (Exception e) {
                log.warn("Failed to create task for finding: {}", title, e);
            }
        }
        return created;
    }

    private String buildTaskTitle(SecurityFinding finding) {
        return switch (finding.getScanner()) {
            case GITLEAKS -> {
                String file = finding.getFilePath();
                String shortFile = file.contains("/") ? file.substring(file.lastIndexOf('/') + 1) : file;
                yield "[GITLEAKS] " + finding.getTitle() + " in " + shortFile + ":" + finding.getStartLine();
            }
            case OPENGREP -> {
                String file = finding.getFilePath();
                String shortFile = file.contains("/") ? file.substring(file.lastIndexOf('/') + 1) : file;
                yield "[OPENGREP] " + finding.getTitle() + " in " + shortFile + ":" + finding.getStartLine();
            }
            case TRIVY -> "[TRIVY] " + finding.getRuleId() + " in " +
                    finding.getPackageName() + "@" + finding.getInstalledVersion();
        };
    }

    private List<String> buildLabels(SecurityFinding finding) {
        List<String> labels = new ArrayList<>();
        labels.add("security");
        labels.add(finding.getScanner().getId());
        labels.add(finding.getSeverity());
        return labels;
    }

    private void notifyOnEdt(@Nullable SecurityScanListener listener,
                              java.util.function.Consumer<SecurityScanListener> action) {
        if (listener != null) {
            ApplicationManager.getApplication().invokeLater(() -> action.accept(listener));
        }
    }

    @Override
    public void dispose() {
        cancelled = true;
    }

    private record ScannerEntry(ScannerType type, AbstractScanner scanner, String binaryPath) {}
}
