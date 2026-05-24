package com.devoxx.genie.ui.panel.ap;

import com.devoxx.genie.model.ap.ApAgent;
import com.devoxx.genie.model.ap.ApProject;
import com.devoxx.genie.model.ap.ApRunEvent;
import com.devoxx.genie.model.ap.ApRunHandle;
import com.devoxx.genie.model.ap.ApSession;
import com.devoxx.genie.service.ap.ApCliException;
import com.devoxx.genie.service.ap.ApCliService;
import com.devoxx.genie.service.ap.ApProjectMatcher;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.devoxx.genie.ui.component.border.AnimatedGlowingBorder;
import com.devoxx.genie.ui.component.button.AddFilesToContextButton;
import com.devoxx.genie.ui.component.button.ButtonFactory;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * "New Run" tab — pick an agent + project, type a prompt, stream the response back.
 *
 * <p>Working directory is sourced from {@link Project#getBasePath()} and passed via
 * {@code --working-dir} on every {@code ap run} invocation so the remote agent sees
 * the right project root.</p>
 */
@Slf4j
public class ApNewRunPanel extends JPanel {

    private final Project project;

    private static final String PROMPT_PLACEHOLDER = "Type prompt";
    private static final String NEW_PROJECT_URL = "https://agentic-platform.docker.com/projects/new";

    private final JComboBox<ApAgent> agentCombo = new JComboBox<>();
    private final JComboBox<ApProject> projectCombo = new JComboBox<>();
    /**
     * Multi-line prompt input. Subclassed to paint {@link #PROMPT_PLACEHOLDER} when empty,
     * mirroring {@code CommandAutoCompleteTextField}'s placeholder paint trick without
     * pulling in the chat's slash-command auto-complete behaviour.
     */
    private final JBTextArea promptArea = new JBTextArea(5, 60) {
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                g.setColor(JBColor.GRAY);
                g.drawString(PROMPT_PLACEHOLDER,
                        getInsets().left,
                        g.getFontMetrics().getMaxAscent() + getInsets().top);
            }
        }
    };
    private final JEditorPane outputArea = new JEditorPane("text/html", "");
    /** Accumulates raw markdown across stream chunks; re-rendered into {@link #outputArea} on every update. */
    private final StringBuilder markdownBuffer = new StringBuilder();
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
    private static final String SUBMIT_TOOLTIP = "Submit prompt to agent";
    private static final String STOP_TOOLTIP = "Stop the current run";
    private static final String POLLING_TOOLTIP = "Polling spawned sessions — click to stop watching (results stay visible)";

    /** Tri-state for the run button: idle, run-in-flight, or watching spawned sub-sessions. */
    private enum RunState { SUBMIT, RUNNING, POLLING }
    private RunState runState = RunState.SUBMIT;

    /**
     * Tri-state toggling button:
     * <ul>
     *   <li>{@link RunState#SUBMIT} — paper-plane icon; click runs {@link #submit()}.</li>
     *   <li>{@link RunState#RUNNING} — stop icon; click runs {@link #cancel()}.</li>
     *   <li>{@link RunState#POLLING} — clock icon; click stops follow-up polling while
     *       leaving the response output visible (see {@link #stopPollingByUser()}).</li>
     * </ul>
     */
    private final JButton submitBtn = ButtonFactory.createActionButton(
            DevoxxGenieIconsUtil.SubmitIcon, SUBMIT_TOOLTIP, e -> onButtonClicked());
    private final JButton refreshDropdownsBtn = new JButton(DevoxxGenieIconsUtil.RefreshIcon);
    private final AddFilesToContextButton addFilesBtn;
    private final HyperlinkLabel openInBrowserLink = new HyperlinkLabel(" ");
    private final JBLabel statusLabel = new JBLabel(" ");

    private @Nullable ApCliService.ApRunHandleRef activeRun;

    /**
     * Pulsing blue border applied to the whole panel while a run is streaming — matches the
     * effect on the chat tab so both tabs share a "something is running" visual.
     */
    private final AnimatedGlowingBorder animatedBorder = new AnimatedGlowingBorder(this);

    /** Captured from {@link #onHandle} so we can exclude the orchestrator from the follow-up poll. */
    private @Nullable String orchestratorSessionId;
    /** Name of the AP project the current/most-recent run was launched against. */
    private @Nullable String activeProjectName;
    /** Markdown appended after {@link #markdownBuffer} that summarises spawned-session progress. */
    private @NotNull String progressTail = "";
    /** Live poller that watches sessions spawned by the orchestrator after the run's stream ends. */
    private @Nullable ScheduledFuture<?> followupPoller;
    /**
     * Snapshot of session IDs that existed in the project before this run started.
     * Used by {@link #pollOnce} to distinguish freshly-spawned sub-sessions from older sessions
     * (e.g. previous {@code WAITING_FOR_USER_INPUT} sessions) in the same project.
     * {@code null} until the snapshot completes; volatile because it's written from a pooled
     * thread and read from the scheduler thread.
     */
    private volatile @Nullable Set<String> preRunSessionIds;
    /**
     * Wall-clock timestamp at which {@link #onComplete} fired, or {@code null} while the run
     * is still streaming. The follow-up poller uses this to decide when the post-run grace
     * window starts.
     */
    private volatile @Nullable Long runEndedMillis;

    private static final long POLL_INTERVAL_SECONDS = 5;
    /** Hard upper bound — stop polling after this regardless of session states, to avoid leaks. */
    private static final long MAX_POLL_DURATION_SECONDS = 600;
    /** Grace window after the run ends; if no PROCESSING sub-sessions appear by then, stop polling. */
    private static final long POLL_GRACE_SECONDS = 30;

    public ApNewRunPanel(@NotNull Project project) {
        super(new BorderLayout());
        this.project = project;
        this.addFilesBtn = new AddFilesToContextButton(project, this::insertFilePathIntoPrompt);

        agentCombo.setRenderer(new AgentRenderer());
        projectCombo.setRenderer(new ProjectRenderer());

        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setEditorKit(buildStyledHtmlKit(promptArea.getFont()));
        outputArea.setText("");
        // Spawned-session links rendered into the markdown are only useful if they actually
        // open the browser — the CLI offers no way to read another session's output, so the
        // browser is the only escape hatch.
        outputArea.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                URL url = e.getURL();
                if (url != null) BrowserUtil.open(url.toString());
            }
        });

        setSubmitMode();
        openInBrowserLink.setVisible(false);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterSplitter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        refreshDropdowns();
    }

    /**
     * Inserts the picked file's project-relative path at the prompt cursor (with surrounding
     * whitespace if needed). The remote agent runs in {@code --working-dir = project base path},
     * so a relative path is what it'll actually be able to resolve.
     */
    private void insertFilePathIntoPrompt(@NotNull VirtualFile file) {
        String basePath = project.getBasePath();
        String filePath = file.getPath();
        String rendered;
        if (basePath != null && filePath.startsWith(basePath + "/")) {
            rendered = filePath.substring(basePath.length() + 1);
        } else {
            rendered = filePath;
        }
        int caret = promptArea.getCaretPosition();
        String existing = promptArea.getText();
        String prefix = (caret > 0 && !Character.isWhitespace(existing.charAt(caret - 1))) ? " " : "";
        String suffix = (caret < existing.length() && !Character.isWhitespace(existing.charAt(caret))) ? " " : "";
        promptArea.insert(prefix + rendered + suffix, caret);
        promptArea.requestFocusInWindow();
    }

    /** Puts focus on the prompt area whenever this tab is shown. */
    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(promptArea::requestFocusInWindow);
    }

    private @NotNull JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JBLabel("Agent:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.5;
        panel.add(agentCombo, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JBLabel("Project:"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.5;
        panel.add(projectCombo, gbc);

        refreshDropdownsBtn.addActionListener(e -> refreshDropdowns());
        gbc.gridx = 4; gbc.weightx = 0;
        panel.add(refreshDropdownsBtn, gbc);

        return panel;
    }

    private @NotNull JBSplitter buildCenterSplitter() {
        JPanel promptPanel = new JPanel(new BorderLayout());
        promptPanel.add(new JBScrollPane(promptArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout());
        JPanel left = new JPanel();
        left.add(submitBtn);
        left.add(addFilesBtn);
        buttons.add(left, BorderLayout.WEST);
        promptPanel.add(buttons, BorderLayout.SOUTH);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JBScrollPane(outputArea), BorderLayout.CENTER);

        JBSplitter splitter = new JBSplitter(true, 0.3f);
        splitter.setFirstComponent(promptPanel);
        splitter.setSecondComponent(outputPanel);
        return splitter;
    }

    private @NotNull JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(openInBrowserLink, BorderLayout.EAST);
        return bar;
    }

    public void refreshDropdowns() {
        statusLabel.setText("Loading agents and projects…");
        setRefreshingDropdowns(true);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApCliService svc = ApCliService.getInstance();
                List<ApAgent> agents = svc.listAgents(50);
                List<ApProject> projects = svc.listProjects(50);
                ApplicationManager.getApplication().invokeLater(() -> {
                    agentCombo.setModel(new DefaultComboBoxModel<>(agents.toArray(new ApAgent[0])));
                    projectCombo.setModel(new DefaultComboBoxModel<>(projects.toArray(new ApProject[0])));
                    preselectCurrentProject(projects);
                    statusLabel.setText("Ready — " + agents.size() + " agent(s), " + projects.size() + " project(s)");
                    if (projects.isEmpty()) {
                        showNoProjectsMessage();
                    }
                    setRefreshingDropdowns(false);
                }, ModalityState.any());
            } catch (ApCliException e) {
                log.warn("Failed to load agents/projects: {}", e.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> {
                    statusLabel.setText("Error loading agents/projects — see response panel.");
                    showError("Error loading agents and projects", e.getMessage());
                    setRefreshingDropdowns(false);
                }, ModalityState.any());
            }
        });
    }

    /** Swaps the refresh button icon for a spinner (and disables the button) while a refresh is in flight. */
    private void setRefreshingDropdowns(boolean refreshing) {
        refreshDropdownsBtn.setIcon(refreshing ? AnimatedIcon.Default.INSTANCE : DevoxxGenieIconsUtil.RefreshIcon);
        refreshDropdownsBtn.setEnabled(!refreshing);
    }

    /** Routes multi-line / verbose error text into the Response area, rendered as markdown. */
    private void showError(@NotNull String headline, @Nullable String detail) {
        markdownBuffer.setLength(0);
        markdownBuffer.append("### ").append(headline).append("\n\n");
        if (detail != null && !detail.isBlank()) {
            // Fence the detail so any `*`/`#` chars from the CLI render verbatim, not as markdown.
            markdownBuffer.append("```\n").append(detail).append("\n```\n");
        }
        renderMarkdown();
        outputArea.setCaretPosition(0);
    }

    /**
     * Renders a call-to-action in the response area when the account has no Agentic Platform
     * projects yet. The link is opened in the browser by the output pane's hyperlink listener.
     */
    private void showNoProjectsMessage() {
        markdownBuffer.setLength(0);
        progressTail = "";
        markdownBuffer.append("### No Docker Agentic Platform projects found\n\n")
                .append("You don't have any Agentic Platform projects yet. ")
                .append("[Create a new project](").append(NEW_PROJECT_URL).append(") ")
                .append("to get started, then click Refresh.\n");
        renderMarkdown();
        outputArea.setCaretPosition(0);
    }

    /** Pre-selects the AP project whose name matches the current IDE project (see {@link ApProjectMatcher}). */
    private void preselectCurrentProject(@NotNull List<ApProject> projects) {
        if (projects.isEmpty()) return;
        for (String candidate : ApProjectMatcher.candidateNames(project)) {
            for (ApProject p : projects) {
                if (p.name() != null && candidate.equalsIgnoreCase(p.name())) {
                    projectCombo.setSelectedItem(p);
                    return;
                }
            }
        }
    }

    private void submit() {
        String prompt = promptArea.getText().trim();
        if (prompt.isEmpty()) {
            statusLabel.setText("Type a prompt first.");
            return;
        }
        ApAgent agent = (ApAgent) agentCombo.getSelectedItem();
        ApProject proj = (ApProject) projectCombo.getSelectedItem();
        if (agent == null || proj == null) {
            statusLabel.setText("Select an agent and a project.");
            return;
        }

        cancelFollowupPolling();
        markdownBuffer.setLength(0);
        progressTail = "";
        orchestratorSessionId = null;
        activeProjectName = proj.name();
        runEndedMillis = null;
        preRunSessionIds = null;
        outputArea.setText("");
        openInBrowserLink.setVisible(false);
        setStopMode();
        statusLabel.setText("Starting run…");

        snapshotPreRunSessionIds(proj.name());

        activeRun = ApCliService.getInstance().startRun(
                prompt,
                agent.name(),
                proj.name(),
                project.getBasePath(),
                this::onHandle,
                this::onEvent,
                this::onComplete);
    }

    /**
     * Lists existing sessions for the project on a pooled thread and stashes their IDs in
     * {@link #preRunSessionIds}. Runs in parallel with {@code ap run} start-up; the result is
     * almost always ready by the time {@link #onHandle} fires (and {@link #pollOnce} waits
     * for it before treating anything as a spawned session). Failures fall back to an empty
     * set so we don't block polling forever — the worst case is that an old session briefly
     * appears in the spawned list until the orchestrator filter excludes it.
     */
    private void snapshotPreRunSessionIds(@NotNull String projectName) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Set<String> ids = new HashSet<>();
            try {
                for (ApSession s : ApCliService.getInstance().listSessions(null, projectName, 50)) {
                    if (s.id() != null) ids.add(s.id());
                }
            } catch (ApCliException e) {
                log.debug("Pre-run session snapshot failed: {}", e.getMessage());
            }
            preRunSessionIds = Set.copyOf(ids);
        });
    }

    private void cancel() {
        if (activeRun != null) {
            activeRun.cancel();
            statusLabel.setText("Cancelling…");
        }
    }

    /** Routes the single button's click based on the current {@link RunState}. */
    private void onButtonClicked() {
        switch (runState) {
            case SUBMIT -> submit();
            case RUNNING -> cancel();
            case POLLING -> stopPollingByUser();
        }
    }

    private void setSubmitMode() {
        runState = RunState.SUBMIT;
        submitBtn.setIcon(DevoxxGenieIconsUtil.SubmitIcon);
        submitBtn.setToolTipText(SUBMIT_TOOLTIP);
        animatedBorder.stopGlowing();
    }

    private void setStopMode() {
        runState = RunState.RUNNING;
        submitBtn.setIcon(DevoxxGenieIconsUtil.StopIcon);
        submitBtn.setToolTipText(STOP_TOOLTIP);
        animatedBorder.startGlowing();
    }

    private void setPollingMode() {
        runState = RunState.POLLING;
        submitBtn.setIcon(DevoxxGenieIconsUtil.ClockIcon);
        submitBtn.setToolTipText(POLLING_TOOLTIP);
        animatedBorder.stopGlowing();
    }

    /**
     * User clicked the button while in {@link RunState#POLLING}: stop the follow-up watcher
     * but leave the response output intact so the user can keep reading what came back.
     */
    private void stopPollingByUser() {
        cancelFollowupPolling();
        setSubmitMode();
        statusLabel.setText("Stopped watching for spawned sessions.");
    }

    private void onHandle(@NotNull ApRunHandle handle) {
        orchestratorSessionId = handle.sessionId();
        statusLabel.setText("Session " + safe(handle.sessionId()) + " started.");
        String url = handle.openUrl();
        if (url != null && !url.isBlank()) {
            openInBrowserLink.setHyperlinkText("Open session in browser");
            openInBrowserLink.setHyperlinkTarget(url);
            openInBrowserLink.setVisible(true);
        }
        // Begin watching for spawned sub-sessions immediately. The orchestrator session ID is
        // now known, so the poller can filter it out from day one; siblings (e.g. reviewer,
        // architect) will appear in the response area while the orchestrator is still talking.
        startFollowupPolling();
    }

    private void onEvent(@NotNull ApRunEvent event) {
        if (event instanceof ApRunEvent.AgentOutput out) {
            markdownBuffer.append(out.content());
            renderMarkdown();
        } else if (event instanceof ApRunEvent.StreamStopped) {
            // StreamStopped fires when the orchestrator's stream block ends, which can happen
            // before the run itself terminates (e.g. while it's waiting for sub-agents). If we
            // are still polling for spawned sessions, "Stream finished" reads as "nothing's
            // happening" — surface the actual ongoing work instead.
            statusLabel.setText(followupPoller != null
                    ? "Watching for spawned sessions…"
                    : "Stream finished.");
        }
        // StreamStarted / Other intentionally ignored in the visible response area.
    }

    /**
     * Builds an {@link HTMLEditorKit} whose stylesheet pins the body font to the supplied
     * font (typically the prompt input's font, so the rendered response matches what the
     * user typed). The default kit otherwise renders HTML with a small Times-style font
     * that looks out of place next to the rest of the IDE UI.
     *
     * <p>Swing's {@link javax.swing.text.html.CSS} only understands a tiny subset of CSS;
     * shorthand colors, {@code rgba(...)} and {@code border-radius} all crash {@code addRule}.
     * Stick to plain properties with hex/named colors and add each rule individually so a
     * single bad rule never poisons the whole stylesheet.</p>
     */
    private static @NotNull HTMLEditorKit buildStyledHtmlKit(@NotNull Font base) {
        HTMLEditorKit kit = new HTMLEditorKit();
        String family = base.getFamily();
        int size = base.getSize();
        StyleSheet css = kit.getStyleSheet();
        addRuleSafe(css, String.format("body { font-family: %s; font-size: %dpt; margin: 4px 6px; }", family, size));
        addRuleSafe(css, "p { margin: 4px 0; }");
        addRuleSafe(css, "h1 { margin: 8px 0 4px 0; }");
        addRuleSafe(css, "h2 { margin: 8px 0 4px 0; }");
        addRuleSafe(css, "h3 { margin: 8px 0 4px 0; }");
        addRuleSafe(css, "h4 { margin: 8px 0 4px 0; }");
        addRuleSafe(css, String.format("code { font-family: %s; font-size: %dpt; }", family, size));
        addRuleSafe(css, String.format("pre { font-family: %s; font-size: %dpt; margin: 6px 0; }", family, size));
        addRuleSafe(css, "ul { margin: 4px 0 4px 20px; }");
        addRuleSafe(css, "ol { margin: 4px 0 4px 20px; }");
        return kit;
    }

    private static void addRuleSafe(@NotNull StyleSheet css, @NotNull String rule) {
        try {
            css.addRule(rule);
        } catch (RuntimeException ex) {
            log.debug("Skipping unsupported CSS rule for AP response pane: {}", rule);
        }
    }

    /**
     * Re-renders the accumulated markdown into the output pane.
     * Called on every streamed chunk — CommonMark's parser is fast enough that re-parsing
     * the whole buffer per chunk is the simplest way to keep markdown correct under streaming
     * (a chunk can split a fenced code block or a list item, so incremental rendering is brittle).
     */
    private void renderMarkdown() {
        String source = progressTail.isEmpty() ? markdownBuffer.toString() : markdownBuffer + progressTail;
        String html = htmlRenderer.render(markdownParser.parse(source));
        outputArea.setText(html);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void onComplete(@NotNull ApCliService.RunCompletion completion) {
        activeRun = null;
        runEndedMillis = System.currentTimeMillis();
        if (!completion.ok()) {
            cancelFollowupPolling();
            setSubmitMode();
            statusLabel.setText("Run failed — see response panel.");
            showError("Run failed", completion.error());
        } else if (statusLabel.getText().startsWith("Cancelling")) {
            cancelFollowupPolling();
            setSubmitMode();
            statusLabel.setText("Run cancelled.");
        } else if (followupPoller != null) {
            // Polling was started at onHandle and is still running — slide into POLLING mode
            // so the user can see we're still watching siblings, and click to stop.
            setPollingMode();
            statusLabel.setText("Run completed — watching for spawned sessions…");
        } else {
            setSubmitMode();
            statusLabel.setText("Run completed.");
        }
    }

    /**
     * Watches for sessions the orchestrator spawned (sub-agents) that are still {@code PROCESSING}
     * in the same project, after the {@code ap run} stream has ended. The CLI doesn't expose a
     * way to follow content of those sessions, so we surface their state only — letting the user
     * know work is still happening and pointing them to the Sessions tab once everything settles.
     *
     * <p>Polls every {@value #POLL_INTERVAL_SECONDS}s, gives up if no in-progress sub-sessions
     * appear within {@value #POLL_GRACE_SECONDS}s, and hard-stops after {@value #MAX_POLL_DURATION_SECONDS}s.</p>
     */
    private void startFollowupPolling() {
        if (activeProjectName == null) return;
        cancelFollowupPolling();
        long startMillis = System.currentTimeMillis();
        followupPoller = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
                () -> pollOnce(startMillis),
                POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void pollOnce(long startMillis) {
        String projectName = activeProjectName;
        if (projectName == null) {
            stopPollingAuto(null);
            return;
        }
        long now = System.currentTimeMillis();
        long elapsedSeconds = (now - startMillis) / 1000;
        if (elapsedSeconds > MAX_POLL_DURATION_SECONDS) {
            ApplicationManager.getApplication().invokeLater(() -> {
                progressTail = "\n\n---\n\n*Stopped following spawned sessions after "
                        + (MAX_POLL_DURATION_SECONDS / 60) + " minutes. "
                        + "Open the **Sessions** tab to check on them.*\n";
                renderMarkdown();
            }, ModalityState.any());
            stopPollingAuto("Stopped watching — time limit reached.");
            return;
        }

        // Wait until the pre-run snapshot is available — without it we'd flag every existing
        // session in the project as "spawned" and pollute the response area.
        Set<String> preExisting = preRunSessionIds;
        if (preExisting == null) return;

        List<ApSession> sessions;
        try {
            sessions = ApCliService.getInstance().listSessions(null, projectName, 50);
        } catch (ApCliException e) {
            log.debug("Follow-up poll failed: {}", e.getMessage());
            return; // transient — try again next tick
        }

        List<ApSession> spawned = new java.util.ArrayList<>();
        for (ApSession s : sessions) {
            if (s.id() == null) continue;
            if (s.id().equals(orchestratorSessionId)) continue;
            if (preExisting.contains(s.id())) continue;
            spawned.add(s);
        }
        boolean anyProcessing = spawned.stream().anyMatch(s -> "PROCESSING".equalsIgnoreCase(s.status()));

        // Always reflect what we currently see — including DURING the run, so the user can
        // click into a sibling session while the orchestrator is still streaming.
        String tail = spawned.isEmpty() ? "" : renderProgressTail(spawned, anyProcessing);
        ApplicationManager.getApplication().invokeLater(() -> {
            progressTail = tail;
            renderMarkdown();
        }, ModalityState.any());

        // Stop-conditions only apply once the run itself has ended. While the orchestrator is
        // still streaming, we keep polling no matter what so newly-spawned siblings show up.
        Long endedAt = runEndedMillis;
        if (endedAt == null) return;
        long sinceRunEnded = (now - endedAt) / 1000;
        if (anyProcessing) return;
        if (sinceRunEnded < POLL_GRACE_SECONDS) return;
        stopPollingAuto(spawned.isEmpty()
                ? "Run completed — no spawned sessions detected."
                : "All spawned sessions finished.");
    }

    /**
     * Called from the polling thread when polling should end on its own (vs. user click).
     * Cancels the scheduler, then flips the button back to SUBMIT on the EDT and optionally
     * updates the status line. The {@code runState} guard avoids stomping on a state the user
     * (or a freshly started run) has already changed in the brief window before this runs.
     */
    private void stopPollingAuto(@Nullable String statusMessage) {
        cancelFollowupPolling();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (runState != RunState.POLLING) return;
            setSubmitMode();
            if (statusMessage != null) statusLabel.setText(statusMessage);
        }, ModalityState.any());
    }

    private static @NotNull String renderProgressTail(@NotNull List<ApSession> spawned, boolean anyProcessing) {
        StringBuilder sb = new StringBuilder("\n\n---\n\n**Spawned sessions");
        sb.append(anyProcessing ? " (in progress):" : " — finished:").append("**\n\n");
        for (ApSession s : spawned) {
            String marker = "PROCESSING".equalsIgnoreCase(s.status()) ? "⏳" : "✓";
            String title = (s.title() == null || s.title().isBlank()) ? "(untitled)" : s.title();
            String agent = s.agent() == null ? "" : " *" + s.agent() + "*";
            String url = s.id() == null ? null : String.format(ApSessionsTab.SESSION_URL_TEMPLATE, s.id());
            String linkSuffix = url == null ? "" : " — [open](" + url + ")";
            sb.append("- ").append(marker).append(agent.isEmpty() ? "" : agent + " — ")
                    .append(title).append(" — `").append(safe(s.status())).append('`')
                    .append(linkSuffix).append('\n');
        }
        if (!anyProcessing) {
            sb.append("\n*Open the **Sessions** tab to view their outputs.*\n");
        }
        return sb.toString();
    }

    private void cancelFollowupPolling() {
        ScheduledFuture<?> f = followupPoller;
        if (f != null) {
            f.cancel(false);
            followupPoller = null;
        }
    }

    @Override
    public void removeNotify() {
        cancelFollowupPolling();
        animatedBorder.stopGlowing();
        super.removeNotify();
    }

    private static @NotNull String safe(@Nullable String s) {
        return s == null ? "?" : s;
    }

    // ===== Renderers =====

    private static class AgentRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ApAgent a) {
                setText(a.name());
                setToolTipText(a.description());
            } else {
                setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }

    private static class ProjectRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ApProject p) {
                setText(p.name());
                setToolTipText(p.description());
            } else {
                setText(value == null ? "" : value.toString());
            }
            return this;
        }
    }

    /** Used by the parent panel to pre-select an agent (e.g., from the Agents tab). */
    public void selectAgentByName(@NotNull String name) {
        int n = agentCombo.getItemCount();
        for (int i = 0; i < n; i++) {
            ApAgent a = agentCombo.getItemAt(i);
            if (a != null && name.equals(a.name())) {
                agentCombo.setSelectedIndex(i);
                return;
            }
        }
    }
}
