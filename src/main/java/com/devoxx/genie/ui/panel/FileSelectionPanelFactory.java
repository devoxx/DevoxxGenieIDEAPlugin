package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.window.ConversationTabRegistry;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.application.ApplicationManager;
import com.devoxx.genie.util.ReadAccess;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FileSelectionPanelFactory implements DumbAware {

    private static final int DOUBLE_CLICK = 2;
    private static final int DEBOUNCE_DELAY = 300; // milliseconds
    private static final int MAX_RESULTS = 50;

    private FileSelectionPanelFactory() {
    }

    public static @NotNull JPanel createPanel(Project project, List<VirtualFile> openFiles) {
        return createPanel(project, openFiles, file -> addToFileListManager(project, file));
    }

    /**
     * Variant for callers that need to handle the selected file themselves (e.g. inserting its
     * path into a prompt text area) instead of registering it with {@link FileListManager}.
     */
    public static @NotNull JPanel createPanel(Project project,
                                              List<VirtualFile> openFiles,
                                              @NotNull Consumer<VirtualFile> onSelected) {
        DefaultListModel<VirtualFile> listModel = new DefaultListModel<>();
        JBList<VirtualFile> resultList = createResultList(project, listModel, onSelected);
        JBTextField filterField = createFilterField(project, listModel, resultList, openFiles);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setPreferredSize(new Dimension(400, 300)); // Increased height

        mainPanel.add(filterField, BorderLayout.NORTH);
        JBScrollPane scrollPane = new JBScrollPane(resultList);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Set a preferred size for the scroll pane
        scrollPane.setPreferredSize(new Dimension(380, 250));

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Populate the list model with open files
        for (VirtualFile file : openFiles) {
            listModel.addElement(file);
        }

        // Setup keyboard navigation between filter field and result list
        setupKeyboardNavigation(filterField, resultList, onSelected);

        return mainPanel;
    }

    /**
     * Create the result list
     *
     * @param project the project reference
     * @param listModel the list model
     * @return the list
     */
    private static @NotNull JBList<VirtualFile> createResultList(Project project,
                                                                  DefaultListModel<VirtualFile> listModel,
                                                                  @NotNull Consumer<VirtualFile> onSelected) {
        JBList<VirtualFile> resultList = new JBList<>(listModel);
        resultList.setCellRenderer(new FileListCellRenderer(project));
        resultList.setVisibleRowCount(10); // Show 10 rows by default

        addMouseListenerToResultList(resultList, onSelected);
        return resultList;
    }

    /**
     * Create the filter file field
     *
     * @param project    the project
     * @param listModel  the list model
     * @param resultList the result list
     * @return the filter field
     */
    private static @NotNull JBTextField createFilterField(Project project,
                                                          DefaultListModel<VirtualFile> listModel,
                                                          JBList<VirtualFile> resultList,
                                                          List<VirtualFile> openFiles) {
        JBTextField filterField = new JBTextField();
        filterField.getEmptyText().setText("Type to search for files...");

        AtomicReference<Timer> debounceTimer = new AtomicReference<>(new Timer(DEBOUNCE_DELAY, null));
        debounceTimer.get().setRepeats(false);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceSearch(project, filterField, listModel, resultList, debounceTimer, openFiles);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceSearch(project, filterField, listModel, resultList, debounceTimer, openFiles);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceSearch(project, filterField, listModel, resultList, debounceTimer, openFiles);
            }
        });

        return filterField;
    }

    /**
     * Debounce the search, only search after a DEBOUNCE_DELAY
     *
     * @param project       the project
     * @param filterField   the filter field
     * @param listModel     the list model
     * @param resultList    the result list
     * @param debounceTimer the debounce timer
     */
    private static void debounceSearch(Project project,
                                       JBTextField filterField,
                                       DefaultListModel<VirtualFile> listModel,
                                       JBList<VirtualFile> resultList,
                                       @NotNull AtomicReference<Timer> debounceTimer,
                                       List<VirtualFile> openFiles) {
        debounceTimer.get().stop();
        debounceTimer.set(new Timer(DEBOUNCE_DELAY, e ->
                searchFiles(project, filterField.getText(), listModel, resultList, openFiles)));
        debounceTimer.get().setRepeats(false);
        debounceTimer.get().start();
    }

    /**
     * Search for files
     *
     * @param project    the project
     * @param searchText the search text
     * @param listModel  the list model
     * @param resultList the result list
     */
    private static void searchFiles(Project project,
                                    String searchText,
                                    DefaultListModel<VirtualFile> listModel,
                                    JBList<VirtualFile> resultList,
                                    List<VirtualFile> openFiles) {
        new FileSearchTask(project, searchText, listModel, resultList, openFiles).queue();
    }

    private static final class FileSearchTask extends Task.Backgroundable {

        private final Project project;
        private final String searchText;
        private final DefaultListModel<VirtualFile> listModel;
        private final JBList<VirtualFile> resultList;
        private final List<VirtualFile> openFiles;
        private final List<VirtualFile> foundFiles = new ArrayList<>();

        FileSearchTask(Project project, String searchText, DefaultListModel<VirtualFile> listModel,
                       JBList<VirtualFile> resultList, List<VirtualFile> openFiles) {
            super(project, "Searching files", true);
            this.project = project;
            this.searchText = searchText;
            this.listModel = listModel;
            this.resultList = resultList;
            this.openFiles = openFiles;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            ReadAccess.run(() -> searchVirtualFiles(indicator));
        }

        private void searchVirtualFiles(@NotNull ProgressIndicator indicator) {
            if (searchOpenFiles(indicator)) return;
            if (searchProjectFiles(indicator)) return;
            foundFiles.sort(Comparator.comparing(VirtualFile::getName, String.CASE_INSENSITIVE_ORDER));
        }

        private boolean searchProjectFiles(@NotNull ProgressIndicator indicator) {
            GotoFileModel model = new GotoFileModel(project);
            String[] names = model.getNames(false);
            for (String name : names) {
                if (indicator.isCanceled()) return true;
                if (foundFiles.size() >= MAX_RESULTS) return false;
                if (name.toLowerCase().contains(searchText.toLowerCase())) {
                    addMatchingFiles(model, name);
                    if (foundFiles.size() >= MAX_RESULTS) return false;
                }
            }
            return false;
        }

        private void addMatchingFiles(GotoFileModel model, String name) {
            Object[] objects = model.getElementsByName(name, false, name);
            for (Object obj : objects) {
                if (obj instanceof PsiFile psiFile) {
                    VirtualFile virtualFile = psiFile.getVirtualFile();
                    if (virtualFile != null && !foundFiles.contains(virtualFile)) {
                        foundFiles.add(virtualFile);
                    }
                }
            }
        }

        private boolean searchOpenFiles(@NotNull ProgressIndicator indicator) {
            for (VirtualFile file : openFiles) {
                if (indicator.isCanceled()) return true;
                if (file.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    foundFiles.add(file);
                }
            }
            return false;
        }

        @Override
        public void onSuccess() {
            ApplicationManager.getApplication().invokeLater(() -> {
                listModel.clear();
                for (VirtualFile file : foundFiles) {
                    listModel.addElement(file);
                }
                resultList.updateUI();
            });
        }
    }

    private static void addMouseListenerToResultList(@NotNull JBList<VirtualFile> resultList,
                                                     @NotNull Consumer<VirtualFile> onSelected) {
        resultList.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == DOUBLE_CLICK) {
                    deliverSelection(resultList, onSelected);
                }
            }
        });
    }

    private static void deliverSelection(@NotNull JBList<VirtualFile> resultList,
                                         @NotNull Consumer<VirtualFile> onSelected) {
        VirtualFile selectedFile = resultList.getSelectedValue();
        if (selectedFile != null) {
            onSelected.accept(selectedFile);
        }
    }

    /**
     * Default handler used by the original two-argument {@link #createPanel} entry point:
     * registers the selected file with the chat's persistent file list for the active tab.
     */
    private static void addToFileListManager(@NotNull Project project, @NotNull VirtualFile file) {
        FileListManager fileListManager = FileListManager.getInstance();
        String tabId = ConversationTabRegistry.getInstance().getActiveTabId(project);
        if (!fileListManager.contains(project, tabId, file)) {
            fileListManager.addFile(project, tabId, file);
        }
    }

    private static void setupKeyboardNavigation(JBTextField filterField,
                                                JBList<VirtualFile> resultList,
                                                @NotNull Consumer<VirtualFile> onSelected) {
        filterField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleFilterFieldKeyPressed(e, resultList, onSelected);
            }
        });

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleResultListKeyPressed(e, filterField, resultList, onSelected);
            }
        });
    }

    /** Test-only overload that uses the default {@link FileListManager}-backed selection handler. */
    static void handleFilterFieldKeyPressed(KeyEvent e, JBList<VirtualFile> resultList, @NotNull Project project) {
        handleFilterFieldKeyPressed(e, resultList, file -> addToFileListManager(project, file));
    }

    /** Test-only overload that uses the default {@link FileListManager}-backed selection handler. */
    static void handleResultListKeyPressed(KeyEvent e, JBTextField filterField,
                                           JBList<VirtualFile> resultList, @NotNull Project project) {
        handleResultListKeyPressed(e, filterField, resultList, file -> addToFileListManager(project, file));
    }

    static void handleFilterFieldKeyPressed(KeyEvent e, JBList<VirtualFile> resultList,
                                            @NotNull Consumer<VirtualFile> onSelected) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN && resultList.getModel().getSize() > 0) {
            resultList.requestFocusInWindow();
            if (resultList.getSelectedIndex() == -1) {
                resultList.setSelectedIndex(0);
            }
            e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && resultList.getModel().getSize() > 0) {
            if (resultList.getSelectedIndex() == -1) {
                resultList.setSelectedIndex(0);
            }
            deliverSelection(resultList, onSelected);
            e.consume();
        }
    }

    static void handleResultListKeyPressed(KeyEvent e, JBTextField filterField,
                                           JBList<VirtualFile> resultList,
                                           @NotNull Consumer<VirtualFile> onSelected) {
        if (e.getKeyCode() == KeyEvent.VK_UP && resultList.getSelectedIndex() == 0) {
            filterField.requestFocusInWindow();
            e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            deliverSelection(resultList, onSelected);
            e.consume();
        }
    }
}
