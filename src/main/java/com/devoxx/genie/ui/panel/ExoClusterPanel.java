package com.devoxx.genie.ui.panel;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.HttpClientProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.JBUI;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.devoxx.genie.util.HttpUtil.ensureEndsWithSlash;

/**
 * Collapsible panel that displays Exo cluster node status above the chat window.
 * Header bar always shows instance status; node cards can be toggled.
 */
public class ExoClusterPanel extends JPanel implements Disposable {

    private static final Gson gson = new Gson();
    private static final JBColor NODE_BG = new JBColor(new Color(45, 45, 48), new Color(45, 45, 48));
    private static final JBColor NODE_BORDER = new JBColor(new Color(80, 80, 85), new Color(80, 80, 85));
    private static final JBColor MEMORY_BAR_BG = new JBColor(new Color(60, 60, 65), new Color(60, 60, 65));
    private static final JBColor MEMORY_BAR_FG = new JBColor(new Color(78, 154, 241), new Color(78, 154, 241));
    private static final JBColor ACTIVE_DOT = new JBColor(new Color(80, 200, 80), new Color(80, 200, 80));
    private static final JBColor INACTIVE_DOT = new JBColor(new Color(120, 120, 120), new Color(120, 120, 120));
    private static final JBColor CONNECTION_LINE = new JBColor(new Color(200, 180, 50), new Color(200, 180, 50));
    private static final JBColor INSTANCE_COLOR = new JBColor(new Color(200, 180, 50), new Color(200, 180, 50));
    private static final int HEADER_HEIGHT = 22;

    private final List<NodeInfo> nodes = new ArrayList<>();
    private String activeModelId = null;
    private String instanceStatus = null;
    private ScheduledFuture<?> refreshTask;
    private boolean expanded = true;

    public ExoClusterPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(JBColor.background());
        setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLineBottom(NODE_BORDER),
                JBUI.Borders.empty(0, 8, 2, 8)
        ));
        setVisible(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Click on the header area toggles expand/collapse
                if (e.getY() <= HEADER_HEIGHT) {
                    expanded = !expanded;
                    revalidate();
                    repaint();
                    // Propagate size change to parent
                    Container parent = getParent();
                    if (parent != null) {
                        parent.revalidate();
                        parent.repaint();
                    }
                }
            }
        });

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void startPolling() {
        stopPolling();
        refreshTask = AppExecutorUtil.getAppScheduledExecutorService()
                .scheduleWithFixedDelay(this::refreshState, 0, 5, TimeUnit.SECONDS);
        setVisible(true);
    }

    public void stopPolling() {
        if (refreshTask != null) {
            refreshTask.cancel(false);
            refreshTask = null;
        }
        setVisible(false);
    }

    private void refreshState() {
        try {
            String url = getExoApiBaseUrl();
            Request request = new Request.Builder().url(url + "state").build();
            try (Response response = HttpClientProvider.getClient().newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject state = gson.fromJson(response.body().string(), JsonObject.class);
                    parseState(state);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        revalidate();
                        repaint();
                    });
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void parseState(@NotNull JsonObject state) {
        nodes.clear();
        activeModelId = null;
        instanceStatus = null;

        JsonObject identities = state.getAsJsonObject("nodeIdentities");
        JsonObject nodeMemory = state.getAsJsonObject("nodeMemory");
        JsonObject topology = state.getAsJsonObject("topology");
        var topoNodes = topology != null ? topology.getAsJsonArray("nodes") : null;

        if (identities != null) {
            for (var entry : identities.entrySet()) {
                String nodeId = entry.getKey();
                JsonObject info = entry.getValue().getAsJsonObject();

                boolean inTopology = false;
                if (topoNodes != null) {
                    for (var tn : topoNodes) {
                        if (tn.getAsString().equals(nodeId)) {
                            inTopology = true;
                            break;
                        }
                    }
                }
                if (!inTopology) continue;

                NodeInfo node = new NodeInfo();
                node.name = info.get("friendlyName").getAsString();
                node.chip = info.get("chipId").getAsString();

                if (nodeMemory != null && nodeMemory.has(nodeId)) {
                    JsonObject mem = nodeMemory.getAsJsonObject(nodeId);
                    node.ramTotal = mem.getAsJsonObject("ramTotal").get("inBytes").getAsLong();
                    node.ramAvailable = mem.getAsJsonObject("ramAvailable").get("inBytes").getAsLong();
                }

                JsonObject nodeSystem = state.getAsJsonObject("nodeSystem");
                if (nodeSystem != null && nodeSystem.has(nodeId)) {
                    JsonObject sys = nodeSystem.getAsJsonObject(nodeId);
                    node.gpuUsage = sys.has("gpuUsage") ? sys.get("gpuUsage").getAsDouble() : 0;
                    node.temperature = sys.has("temp") ? sys.get("temp").getAsDouble() : 0;
                }

                nodes.add(node);
            }
        }

        JsonObject instances = state.getAsJsonObject("instances");
        if (instances != null && !instances.entrySet().isEmpty()) {
            for (var entry : instances.entrySet()) {
                JsonObject inst = entry.getValue().getAsJsonObject();
                for (var inner : inst.entrySet()) {
                    JsonObject details = inner.getValue().getAsJsonObject();
                    JsonObject shards = details.getAsJsonObject("shardAssignments");
                    if (shards != null && shards.has("modelId")) {
                        activeModelId = shards.get("modelId").getAsString();
                    }
                }
            }

            JsonObject runners = state.getAsJsonObject("runners");
            if (runners != null && !runners.entrySet().isEmpty()) {
                boolean allReady = runners.entrySet().stream()
                        .allMatch(e -> {
                            JsonObject r = e.getValue().getAsJsonObject();
                            return r.has("RunnerReady") || r.has("RunnerRunning");
                        });
                boolean anyWarming = runners.entrySet().stream()
                        .anyMatch(e -> {
                            JsonObject r = e.getValue().getAsJsonObject();
                            return r.has("RunnerWarmingUp") || r.has("RunnerLoading");
                        });
                if (allReady) {
                    instanceStatus = "Ready";
                } else if (anyWarming) {
                    instanceStatus = "Warming up";
                } else {
                    instanceStatus = "Loading";
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (nodes.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // --- Header bar (always visible) ---
        drawHeader(g2);

        // --- Node cards (only when expanded) ---
        if (expanded) {
            int panelWidth = getWidth() - 16;
            int nodeWidth = Math.min(180, (panelWidth - (nodes.size() - 1) * 30) / Math.max(nodes.size(), 1));
            int nodeHeight = 64;
            int startX = (getWidth() - (nodes.size() * nodeWidth + (nodes.size() - 1) * 30)) / 2;
            int startY = HEADER_HEIGHT + 4;

            // Connection lines
            if (nodes.size() > 1) {
                g2.setColor(CONNECTION_LINE);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        0, new float[]{4, 4}, 0));
                for (int i = 0; i < nodes.size() - 1; i++) {
                    int x1 = startX + i * (nodeWidth + 30) + nodeWidth;
                    int x2 = startX + (i + 1) * (nodeWidth + 30);
                    int y = startY + nodeHeight / 2;
                    g2.drawLine(x1, y, x2, y);
                }
                g2.setStroke(new BasicStroke(1));
            }

            // Node cards
            for (int i = 0; i < nodes.size(); i++) {
                NodeInfo node = nodes.get(i);
                int x = startX + i * (nodeWidth + 30);
                drawNodeCard(g2, node, x, startY, nodeWidth, nodeHeight);
            }
        }

        g2.dispose();
    }

    private void drawHeader(Graphics2D g2) {
        int y = 4;

        // Toggle arrow
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        g2.setColor(JBColor.GRAY);
        String arrow = expanded ? "\u25BC" : "\u25B6"; // ▼ or ▶
        g2.drawString(arrow, 4, y + 12);

        // "Exo Cluster" label + node count
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(JBColor.foreground());
        g2.drawString("Exo Cluster", 18, y + 12);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
        g2.setColor(JBColor.GRAY);
        g2.drawString(nodes.size() + " node" + (nodes.size() != 1 ? "s" : ""), 90, y + 12);

        // Instance status on the right
        if (activeModelId != null) {
            String modelName = activeModelId.contains("/")
                    ? activeModelId.substring(activeModelId.indexOf('/') + 1)
                    : activeModelId;
            String label = modelName + (instanceStatus != null ? " — " + instanceStatus : "");

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
            FontMetrics fm = g2.getFontMetrics();
            int labelWidth = fm.stringWidth(label) + 16;
            int labelX = getWidth() - labelWidth - 12;

            // Status dot
            g2.setColor("Ready".equals(instanceStatus) ? ACTIVE_DOT : INSTANCE_COLOR);
            g2.fillOval(labelX, y + 5, 7, 7);

            // Label
            g2.setColor(INSTANCE_COLOR);
            g2.drawString(label, labelX + 11, y + 12);
        }
    }

    private void drawNodeCard(Graphics2D g2, NodeInfo node, int x, int y, int w, int h) {
        g2.setColor(NODE_BG);
        g2.fillRoundRect(x, y, w, h, 8, 8);

        g2.setColor(NODE_BORDER);
        g2.drawRoundRect(x, y, w, h, 8, 8);

        int padding = 6;
        int textX = x + padding;

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(JBColor.foreground());
        String name = truncate(node.name, g2.getFontMetrics(), w - padding * 2);
        g2.drawString(name, textX, y + 15);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
        g2.setColor(JBColor.GRAY);
        String chip = truncate(node.chip, g2.getFontMetrics(), w - padding * 2);
        g2.drawString(chip, textX, y + 27);

        if (node.ramTotal > 0) {
            int barX = textX;
            int barY = y + 33;
            int barW = w - padding * 2;
            int barH = 8;
            double usedRatio = 1.0 - (double) node.ramAvailable / node.ramTotal;

            g2.setColor(MEMORY_BAR_BG);
            g2.fillRoundRect(barX, barY, barW, barH, 4, 4);

            g2.setColor(MEMORY_BAR_FG);
            g2.fillRoundRect(barX, barY, (int) (barW * usedRatio), barH, 4, 4);

            long usedGB = (node.ramTotal - node.ramAvailable) / (1024L * 1024 * 1024);
            long totalGB = node.ramTotal / (1024L * 1024 * 1024);
            String memText = usedGB + "/" + totalGB + "GB";
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
            g2.setColor(JBColor.GRAY);
            g2.drawString(memText, textX, y + 54);

            if (node.gpuUsage > 0 || node.temperature > 0) {
                String stats = String.format("%.0f%% %.0f\u00B0C", node.gpuUsage * 100, node.temperature);
                int statsWidth = g2.getFontMetrics().stringWidth(stats);
                g2.drawString(stats, x + w - padding - statsWidth, y + 54);
            }
        }

        g2.setColor(node.ramTotal > 0 ? ACTIVE_DOT : INACTIVE_DOT);
        g2.fillOval(x + w - 12, y + 4, 6, 6);
    }

    private String truncate(String text, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(text) <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i) + "..";
            if (fm.stringWidth(truncated) <= maxWidth) return truncated;
        }
        return "..";
    }

    @Override
    public Dimension getPreferredSize() {
        if (nodes.isEmpty()) return new Dimension(0, 0);
        int height = HEADER_HEIGHT;
        if (expanded) {
            height += 72; // node cards
        }
        return new Dimension(super.getPreferredSize().width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private static String getExoApiBaseUrl() {
        String url = DevoxxGenieStateService.getInstance().getExoModelUrl();
        if (url.endsWith("/v1/")) {
            url = url.substring(0, url.length() - 3);
        } else if (url.endsWith("/v1")) {
            url = url.substring(0, url.length() - 2);
        }
        return ensureEndsWithSlash(url);
    }

    @Override
    public void dispose() {
        stopPolling();
    }

    private static class NodeInfo {
        String name = "";
        String chip = "";
        long ramTotal = 0;
        long ramAvailable = 0;
        double gpuUsage = 0;
        double temperature = 0;
    }
}
