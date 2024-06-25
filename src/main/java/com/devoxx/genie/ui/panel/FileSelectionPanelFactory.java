package com.devoxx.genie.ui.panel;

import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.util.FileTypeIconUtil;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
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
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FileSelectionPanelFactory implements DumbAware {

    private static final int DOUBLE_CLICK = 2;
    private static final int DEBOUNCE_DELAY = 300; // milliseconds

    private FileSelectionPanelFactory() {
    }

    public static @NotNull JPanel createPanel(Project project) {
        DefaultListModel<VirtualFile> listModel = new DefaultListModel<>();
        JBList<VirtualFile> resultList = createResultList(project, listModel);
        JBTextField filterField = createFilterField(project, listModel, resultList);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setMaximumSize(new Dimension(400, 400));

        mainPanel.add(filterField, BorderLayout.NORTH);
        JBScrollPane jScrollPane = new JBScrollPane(resultList);
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(jScrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private static @NotNull JBList<VirtualFile> createResultList(Project project,
                                                                 DefaultListModel<VirtualFile> listModel) {
        JBList<VirtualFile> resultList = new JBList<>(listModel);
        resultList.setCellRenderer(new FileListCellRenderer(project));

        addMouseListenerToResultList(resultList);
        return resultList;
    }

    private static @NotNull JBTextField createFilterField(Project project,
                                                          DefaultListModel<VirtualFile> listModel,
                                                          JBList<VirtualFile> resultList) {
        JBTextField filterField = new JBTextField();
        filterField.getEmptyText().setText("Type to search for files...");

        AtomicReference<Timer> debounceTimer = new AtomicReference<>(new Timer(DEBOUNCE_DELAY, null));
        debounceTimer.get().setRepeats(false);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceSearch(project, filterField, listModel, resultList, debounceTimer);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceSearch(project, filterField, listModel, resultList, debounceTimer);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceSearch(project, filterField, listModel, resultList, debounceTimer);
            }
        });

        return filterField;
    }

    private static void debounceSearch(Project project,
                                       JBTextField filterField,
                                       DefaultListModel<VirtualFile> listModel,
                                       JBList<VirtualFile> resultList,
                                       @NotNull AtomicReference<Timer> debounceTimer) {
        debounceTimer.get().stop();
        debounceTimer.set(new Timer(DEBOUNCE_DELAY, e -> searchFiles(project, filterField.getText(), listModel, resultList)));
        debounceTimer.get().setRepeats(false);
        debounceTimer.get().start();
    }

    private static void searchFiles(Project project,
                                    String searchText,
                                    DefaultListModel<VirtualFile> listModel,
                                    JBList<VirtualFile> resultList) {
        new Task.Backgroundable(project, "Searching Files", true) {
            private final List<VirtualFile> foundFiles = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ReadAction.run(() -> {
                    GotoFileModel model = new GotoFileModel(project);
                    String[] names = model.getNames(false);
                    for (String name : names) {
                        if (indicator.isCanceled()) return;
                        if (name.toLowerCase().contains(searchText.toLowerCase())) {
                            Object[] objects = model.getElementsByName(name, false, name);
                            for (Object obj : objects) {
                                if (obj instanceof PsiFile) {
                                    VirtualFile virtualFile = ((PsiFile) obj).getVirtualFile();
                                    if (virtualFile != null) {
                                        foundFiles.add(virtualFile);
                                    }
                                }
                            }
                        }
                    }
                });
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
        }.queue();
    }

    private static void addMouseListenerToResultList(@NotNull JBList<VirtualFile> resultList) {
        resultList.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == DOUBLE_CLICK) {
                    addSelectedFile(resultList);
                }
            }
        });
    }

    private static void addSelectedFile(@NotNull JBList<VirtualFile> resultList) {
        VirtualFile selectedFile = resultList.getSelectedValue();
        if (selectedFile != null) {
            FileListManager.getInstance().addFile(selectedFile);
        }
    }

    private static class FileListCellRenderer extends DefaultListCellRenderer {
        private final Project project;

        public FileListCellRenderer(Project project) {
            this.project = project;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof VirtualFile file) {
                label.setIcon(FileTypeIconUtil.getFileTypeIcon(project, file));
                label.setText(file.getName());
            }

            return label;
        }
    }
}
