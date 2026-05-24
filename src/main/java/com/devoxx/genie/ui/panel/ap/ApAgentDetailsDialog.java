package com.devoxx.genie.ui.panel.ap;

import com.devoxx.genie.model.ap.ApAgent;
import com.devoxx.genie.model.ap.ApSession;
import com.devoxx.genie.service.ap.ApCliException;
import com.devoxx.genie.service.ap.ApCliService;
import com.devoxx.genie.ui.util.DevoxxGenieIconsUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Read-only details view for an Agentic Platform agent. Shown when the user
 * double-clicks an agent row on the Agents tab.
 *
 * <p>Fields visible per agent are limited by what {@code ap agent ls --json} exposes:
 * just {@code id}, {@code name}, and {@code description}. The dialog enriches that with
 * a list of recent sessions that used this agent (queried via {@code ap session ls --agent}),
 * plus an action to start a new run with this agent pre-selected.</p>
 */
@Slf4j
public class ApAgentDetailsDialog extends DialogWrapper {

    private static final String SESSION_URL_TEMPLATE = "https://agentic-platform.docker.com/sessions/%s";
    private static final String AGENT_URL_TEMPLATE = "https://agentic-platform.docker.com/agents/%s";

    private final ApAgent agent;
    private final @Nullable Consumer<ApAgent> onStartNewRun;

    private final SessionMiniTableModel recentModel = new SessionMiniTableModel();
    private final JBTable recentTable = new JBTable(recentModel);
    private final JBLabel recentStatus = new JBLabel(" ");

    /**
     * @param onStartNewRun callback invoked when the user clicks "Start a new run". {@code null}
     *                      disables that button entirely.
     */
    public ApAgentDetailsDialog(@NotNull ApAgent agent, @Nullable Consumer<ApAgent> onStartNewRun) {
        super(true);
        this.agent = agent;
        this.onStartNewRun = onStartNewRun;
        setTitle("Agent · " + nullSafe(agent.name(), "(unnamed)"));
        setResizable(true);
        init();
        loadRecentSessions();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel root = new JPanel(new BorderLayout());

        JBLabel hint = new JBLabel(
                "<html>The <code>ap</code> CLI exposes only <i>id</i>, <i>name</i>, and <i>description</i>. " +
                "Click <b>Open in web UI</b> to see this agent's model, tools, and system prompt.</html>");
        hint.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
        hint.setFont(hint.getFont().deriveFont(Font.ITALIC));
        root.add(hint, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(JBUI.Borders.empty(8));
        root.add(form, BorderLayout.CENTER);

        GridBagConstraints labelGbc = baseGbc(0);
        labelGbc.anchor = GridBagConstraints.NORTHWEST;

        GridBagConstraints valueGbc = baseGbc(1);
        valueGbc.fill = GridBagConstraints.HORIZONTAL;
        valueGbc.weightx = 1.0;

        int row = 0;
        addRow(form, labelGbc, valueGbc, row++, "Name", readOnlyField(nullSafe(agent.name(), "")));
        addRow(form, labelGbc, valueGbc, row++, "ID", idFieldWithCopy());

        // Description block
        labelGbc.gridy = row;
        form.add(boldLabel("Description"), labelGbc);

        JTextArea descArea = new JTextArea(nullSafe(agent.description(), "(no description provided)"));
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(form.getBackground());
        descArea.setBorder(BorderFactory.createEmptyBorder());

        JBScrollPane descScroll = new JBScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createLineBorder(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()));
        descScroll.setPreferredSize(new Dimension(500, 110));

        GridBagConstraints descGbc = baseGbc(1);
        descGbc.gridy = row++;
        descGbc.fill = GridBagConstraints.BOTH;
        descGbc.weightx = 1.0;
        form.add(descScroll, descGbc);

        // Recent sessions block
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.NORTHWEST;
        JPanel sessionsHeader = new JPanel(new BorderLayout());
        sessionsHeader.setOpaque(false);
        sessionsHeader.add(boldLabel("Recent sessions"), BorderLayout.WEST);
        form.add(sessionsHeader, labelGbc);

        JPanel sessionsBlock = buildRecentSessionsBlock();
        GridBagConstraints sessGbc = baseGbc(1);
        sessGbc.gridy = row;
        sessGbc.fill = GridBagConstraints.BOTH;
        sessGbc.weightx = 1.0;
        sessGbc.weighty = 1.0;
        form.add(sessionsBlock, sessGbc);

        return root;
    }

    private @NotNull JPanel buildRecentSessionsBlock() {
        recentTable.setRowHeight(22);
        recentTable.getTableHeader().setReorderingAllowed(false);
        recentTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        recentTable.getColumnModel().getColumn(0).setPreferredWidth(280);
        recentTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        recentTable.getColumnModel().getColumn(2).setPreferredWidth(140);

        recentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !e.isPopupTrigger()) {
                    int row = recentTable.getSelectedRow();
                    if (row < 0) return;
                    ApSession s = recentModel.getAt(recentTable.convertRowIndexToModel(row));
                    if (s != null && s.id() != null) {
                        BrowserUtil.open(String.format(SESSION_URL_TEMPLATE, s.id()));
                    }
                }
            }
        });

        JBScrollPane scroll = new JBScrollPane(recentTable);
        scroll.setPreferredSize(new Dimension(500, 180));

        recentStatus.setHorizontalAlignment(SwingConstants.LEFT);
        recentStatus.setFont(recentStatus.getFont().deriveFont(Font.ITALIC));

        JPanel block = new JPanel(new BorderLayout(0, 4));
        block.add(scroll, BorderLayout.CENTER);
        block.add(recentStatus, BorderLayout.SOUTH);
        return block;
    }

    private void loadRecentSessions() {
        if (agent.name() == null || agent.name().isBlank()) {
            recentStatus.setText("No agent name — cannot query sessions.");
            return;
        }
        recentStatus.setText("Loading recent sessions…");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<ApSession> sessions = ApCliService.getInstance().listSessions(agent.name(), null, 25);
                ApplicationManager.getApplication().invokeLater(() -> {
                    recentModel.set(sessions);
                    recentStatus.setText(sessions.isEmpty()
                            ? "No sessions found for this agent."
                            : sessions.size() + " session(s) — double-click a row to open in browser.");
                }, ModalityState.any());
            } catch (ApCliException e) {
                log.warn("Failed to list sessions for agent {}: {}", agent.name(), e.getMessage());
                ApplicationManager.getApplication().invokeLater(() ->
                        recentStatus.setText("Could not load sessions: " + e.getMessage()),
                        ModalityState.any());
            }
        });
    }

    @Override
    protected Action @NotNull [] createLeftSideActions() {
        List<Action> actions = new ArrayList<>();
        if (onStartNewRun != null) {
            actions.add(new AbstractAction("Start a new run with this agent") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onStartNewRun.accept(agent);
                    close(OK_EXIT_CODE);
                }
            });
        }
        if (agent.id() != null && !agent.id().isBlank()) {
            actions.add(new AbstractAction("Open in web UI") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BrowserUtil.open(String.format(AGENT_URL_TEMPLATE, agent.id()));
                }
            });
        }
        return actions.toArray(new Action[0]);
    }

    @Override
    protected Action @NotNull [] createActions() {
        // Right-side actions: only Close (no Cancel — this is a read-only view).
        return new Action[]{getOKAction()};
    }

    private @NotNull JComponent idFieldWithCopy() {
        JTextField idField = readOnlyField(nullSafe(agent.id(), ""));
        JButton copy = new JButton("Copy", DevoxxGenieIconsUtil.CopyIcon);
        copy.addActionListener(e -> CopyPasteManager.getInstance()
                .setContents(new StringSelection(nullSafe(agent.id(), ""))));

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(idField, BorderLayout.CENTER);
        row.add(copy, BorderLayout.EAST);
        return row;
    }

    private static @NotNull JTextField readOnlyField(@NotNull String value) {
        JTextField field = new JTextField(value);
        field.setEditable(false);
        field.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return field;
    }

    private static @NotNull JBLabel boldLabel(@NotNull String text) {
        JBLabel label = new JBLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private static @NotNull GridBagConstraints baseGbc(int col) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = col;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(4, col == 0 ? 4 : 0, 4, col == 0 ? 8 : 4);
        return gbc;
    }

    private static void addRow(@NotNull JPanel form,
                                @NotNull GridBagConstraints labelGbc,
                                @NotNull GridBagConstraints valueGbc,
                                int rowIndex,
                                @NotNull String label,
                                @NotNull JComponent value) {
        labelGbc.gridy = rowIndex;
        valueGbc.gridy = rowIndex;
        form.add(boldLabel(label), labelGbc);
        form.add(value, valueGbc);
    }

    private static @NotNull String nullSafe(@Nullable String s, @NotNull String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }

    // ===== Table model =====

    private static class SessionMiniTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Title", "Status", "Project"};
        private final List<ApSession> rows = new ArrayList<>();

        void set(@NotNull List<ApSession> next) {
            rows.clear();
            rows.addAll(next);
            fireTableDataChanged();
        }

        @Nullable ApSession getAt(int row) {
            return row >= 0 && row < rows.size() ? rows.get(row) : null;
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ApSession s = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> s.title() == null ? "" : s.title();
                case 1 -> s.status() == null ? "" : s.status();
                case 2 -> s.project() == null ? "" : s.project();
                default -> "";
            };
        }
    }
}
