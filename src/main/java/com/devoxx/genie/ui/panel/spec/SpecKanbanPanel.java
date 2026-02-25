package com.devoxx.genie.ui.panel.spec;

import com.devoxx.genie.model.spec.AcceptanceCriterion;
import com.devoxx.genie.model.spec.TaskSpec;
import com.devoxx.genie.service.spec.BacklogConfigService;
import com.devoxx.genie.service.spec.SpecService;
import com.devoxx.genie.ui.util.ThemeDetector;
import com.devoxx.genie.ui.webview.JCEFChecker;
import com.devoxx.genie.ui.webview.WebServer;
import com.devoxx.genie.ui.webview.handler.WebViewJavaScriptExecutor;
import com.devoxx.genie.ui.webview.template.KanbanTemplate;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kanban board panel for the Spec Browser tool window.
 * Renders an HTML5 drag-and-drop board in JCEF, or a read-only Swing fallback.
 * Uses lazy initialization — the JCEF browser is only created when the panel first becomes visible.
 * JS→Java communication uses document.title changes caught by CefDisplayHandler:
 *   "SC:{json}"  — status change (drag-drop)
 *   "TC:taskId"  — task card clicked
 *   "RF:true"    — refresh requested
 */
@Slf4j
public class SpecKanbanPanel extends SimpleToolWindowPanel implements Disposable {

    private final transient Project project;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private transient JBCefBrowser browser;
    private transient WebViewJavaScriptExecutor jsExecutor;
    private transient ThemeDetector.ThemeChangeListener themeListener;

    public SpecKanbanPanel(@NotNull Project project) {
        super(true, true);
        this.project = project;

        JBLabel placeholder = new JBLabel("Switch to this tab to load the Kanban board.", SwingConstants.CENTER);
        setContent(placeholder);
    }

    /**
     * Lazy init: create JCEF browser on first addNotify (when panel becomes visible).
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (!initialized.getAndSet(true)) {
            ApplicationManager.getApplication().invokeLater(this::initBrowser);
        }
    }

    private void initBrowser() {
        if (disposed.get()) return;

        if (!JCEFChecker.isJCEFAvailable()) {
            setContent(createSwingFallback());
            return;
        }

        try {
            WebServer webServer = WebServer.getInstance();
            if (!webServer.isRunning()) {
                webServer.start();
            }

            String html = new KanbanTemplate(webServer).generate();
            String resourceId = webServer.addDynamicResource(html);
            String resourceUrl = webServer.getResourceUrl(resourceId);

            browser = new JBCefBrowser();
            browser.getComponent().setMinimumSize(new Dimension(400, 300));

            jsExecutor = new WebViewJavaScriptExecutor(browser);

            browser.loadURL(resourceUrl);

            // Push data once the page finishes loading
            browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
                @Override
                public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                    jsExecutor.setLoaded(true);
                    log.info("Kanban board loaded with status: {}", httpStatusCode);
                    pushDataToBoard();
                }
            }, browser.getCefBrowser());

            // Title-based bridge for JS→Java communication
            setupTitleBridge();

            setContent(browser.getComponent());
            listenForSpecChanges();
            listenForThemeChanges();

        } catch (Exception e) {
            log.error("Failed to initialize Kanban JCEF browser", e);
            setContent(createSwingFallback());
        }
    }

    // ===== Data push =====

    private void pushDataToBoard() {
        if (disposed.get() || jsExecutor == null) return;

        List<TaskSpec> specs = SpecService.getInstance(project).getAllSpecs();
        String tasksJson = serializeTasksToJson(specs);
        String statusesJson = serializeStatusesToJson();

        String js = "updateBoard(" + escapeForJs(tasksJson) + ", " + escapeForJs(statusesJson) + ");";
        jsExecutor.executeJavaScript(js);
    }

    // ===== Title-based communication bridge =====

    private void setupTitleBridge() {
        if (browser == null) return;

        browser.getJBCefClient().addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser cefBrowser, String title) {
                if (title == null || disposed.get()) return;

                if (title.startsWith("SC:")) {
                    handleStatusChange(title.substring(3));
                } else if (title.startsWith("TC:")) {
                    handleTaskClick(title.substring(3));
                } else if (title.startsWith("RF:")) {
                    handleRefreshRequest();
                } else if (title.startsWith("DT:")) {
                    handleDeleteTask(title.substring(3));
                }
            }
        }, browser.getCefBrowser());
    }

    private void handleStatusChange(@NotNull String json) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String taskId = extractJsonValue(json, "taskId");
                String newStatus = extractJsonValue(json, "newStatus");
                if (taskId == null || newStatus == null) return;

                SpecService specService = SpecService.getInstance(project);
                TaskSpec spec = specService.getSpec(taskId);
                if (spec != null && !newStatus.equals(spec.getStatus())) {
                    spec.setStatus(newStatus);
                    specService.updateTask(spec);
                    log.info("Kanban: moved task {} to status {}", taskId, newStatus);
                }
            } catch (Exception e) {
                log.error("Failed to update task status from Kanban drag-drop", e);
            }
        });
    }

    private void handleTaskClick(@NotNull String taskId) {
        ApplicationManager.getApplication().invokeLater(() -> {
            TaskSpec spec = SpecService.getInstance(project).getSpec(taskId);
            if (spec != null && spec.getFilePath() != null) {
                VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(spec.getFilePath());
                if (vf != null) {
                    FileEditorManager.getInstance(project).openFile(vf, true);
                }
            }
        });
    }

    private void handleRefreshRequest() {
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                SpecService.getInstance(project).refresh()
        );
    }

    private void handleDeleteTask(@NotNull String taskId) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SpecService specService = SpecService.getInstance(project);
                TaskSpec spec = specService.getSpec(taskId);
                if (spec != null) {
                    specService.archiveTask(taskId);
                    log.info("Kanban: archived task {} via drag-to-bin", taskId);
                }
            } catch (Exception e) {
                log.error("Failed to archive task from Kanban drag-to-bin", e);
            }
        });
    }

    // ===== Listeners =====

    private void listenForSpecChanges() {
        SpecService.getInstance(project).addChangeListener(() -> {
            if (!disposed.get()) {
                ApplicationManager.getApplication().invokeLater(this::pushDataToBoard);
            }
        });
    }

    private void listenForThemeChanges() {
        themeListener = isDarkTheme -> {
            if (disposed.get() || browser == null) return;
            ApplicationManager.getApplication().invokeLater(this::reloadWithNewTheme);
        };
        ThemeDetector.addThemeChangeListener(themeListener);
    }

    private void reloadWithNewTheme() {
        if (disposed.get() || browser == null) return;

        try {
            WebServer webServer = WebServer.getInstance();
            String html = new KanbanTemplate(webServer).generate();
            String resourceId = webServer.addDynamicResource(html);
            String resourceUrl = webServer.getResourceUrl(resourceId);

            jsExecutor.setLoaded(false);
            browser.loadURL(resourceUrl);
            // onLoadEnd callback will re-push data
        } catch (Exception e) {
            log.error("Failed to reload Kanban board after theme change", e);
        }
    }

    // ===== Swing fallback (no JCEF) =====

    private @NotNull JComponent createSwingFallback() {
        List<String> statuses = getOrderedStatuses();
        JPanel board = new JPanel(new GridLayout(1, statuses.size(), 8, 0));
        board.setBorder(JBUI.Borders.empty(8));

        for (String status : statuses) {
            JPanel column = new JPanel(new BorderLayout());
            column.setBorder(BorderFactory.createTitledBorder(status));

            DefaultListModel<String> listModel = new DefaultListModel<>();
            List<TaskSpec> specs = SpecService.getInstance(project).getSpecsByStatus(status);
            for (TaskSpec spec : specs) {
                listModel.addElement(spec.getDisplayLabel());
            }

            JList<String> list = new JList<>(listModel);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            column.add(new JBScrollPane(list), BorderLayout.CENTER);

            board.add(column);
        }

        return new JBScrollPane(board);
    }

    // ===== JSON serialization =====

    private @NotNull String serializeTasksToJson(@NotNull List<TaskSpec> specs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < specs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(serializeTask(specs.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private @NotNull String serializeTask(@NotNull TaskSpec spec) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(jsonString(spec.getId()));
        sb.append(",\"title\":").append(jsonString(spec.getTitle()));
        sb.append(",\"status\":").append(jsonString(spec.getStatus()));
        sb.append(",\"priority\":").append(jsonString(spec.getPriority()));

        sb.append(",\"labels\":[");
        List<String> labels = spec.getLabels();
        if (labels != null) {
            for (int i = 0; i < labels.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(jsonString(labels.get(i)));
            }
        }
        sb.append("]");

        sb.append(",\"acceptanceCriteria\":[");
        List<AcceptanceCriterion> criteria = spec.getAcceptanceCriteria();
        if (criteria != null) {
            for (int i = 0; i < criteria.size(); i++) {
                if (i > 0) sb.append(",");
                AcceptanceCriterion ac = criteria.get(i);
                sb.append("{\"text\":").append(jsonString(ac.getText()));
                sb.append(",\"checked\":").append(ac.isChecked()).append("}");
            }
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Build the ordered status list from config.yml, then append any extra statuses
     * found on actual tasks (in case a task has a status not listed in config).
     */
    private @NotNull List<String> getOrderedStatuses() {
        List<String> configStatuses = BacklogConfigService.getInstance(project).getConfig().getStatuses();
        List<String> allStatuses = new ArrayList<>(configStatuses != null && !configStatuses.isEmpty()
                ? configStatuses
                : List.of("To Do", "In Progress", "Done"));

        for (String status : SpecService.getInstance(project).getStatuses()) {
            if (!allStatuses.contains(status)) {
                allStatuses.add(status);
            }
        }
        return allStatuses;
    }

    private @NotNull String serializeStatusesToJson() {
        List<String> allStatuses = getOrderedStatuses();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < allStatuses.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(jsonString(allStatuses.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private @NotNull String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private @NotNull String escapeForJs(@NotNull String json) {
        return "'" + json
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "'";
    }

    // ===== JSON parsing utility =====

    private static String extractJsonValue(@NotNull String json, @NotNull String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    // ===== Disposal =====

    @Override
    public void dispose() {
        disposed.set(true);

        if (themeListener != null) {
            ThemeDetector.removeThemeChangeListener(themeListener);
            themeListener = null;
        }

        if (jsExecutor != null) {
            jsExecutor.dispose();
            jsExecutor = null;
        }

        if (browser != null) {
            browser.dispose();
            browser = null;
        }
    }
}
