package com.devoxx.genie.ui.settings.copyproject;

import com.devoxx.genie.ui.settings.AbstractSettingsComponent;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CopyProjectSettingsComponent extends AbstractSettingsComponent {

    private final ExcludedDirectoriesPanel excludedDirectoriesPanel;
    private final ExcludedFilesPanel excludedFilesPanel;  // New panel for excluded files
    private final IncludedFileExtensionsPanel includedFileExtensionsPanel;
    private final JCheckBox excludeJavadocCheckBox;
    private final JCheckBox useGitIgnoreCheckBox;

    public CopyProjectSettingsComponent() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        useGitIgnoreCheckBox = new JCheckBox("Use .gitignore", settings.getUseGitIgnore());
        excludedDirectoriesPanel = new ExcludedDirectoriesPanel(settings.getExcludedDirectories());
        excludedFilesPanel = new ExcludedFilesPanel(settings.getExcludedFiles());  // Initialize the new panel
        includedFileExtensionsPanel = new IncludedFileExtensionsPanel(settings.getIncludedFileExtensions());
        excludeJavadocCheckBox = new JCheckBox("Exclude Javadoc", settings.getExcludeJavaDoc());
    }

    @Override
    public JPanel createPanel() {
        // Add description
        JBLabel descriptionLabel = new JBLabel("<html><body style='width: 100%;'>" +
            "These settings control which files and directories are included when creating a Full Project Context. " +
            "Excluded directories will be skipped, and only files with included extensions " +
            "will be processed. This helps to focus the LLM on relevant code and reduce noise from build artifacts " +
            "or other non-essential files.</body></html>");
        descriptionLabel.setForeground(UIUtil.getContextHelpForeground());
        descriptionLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(descriptionLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(excludedDirectoriesPanel);
        contentPanel.add(excludedFilesPanel);

        contentPanel.add(createUseGitIgnorePanel());

        contentPanel.add(includedFileExtensionsPanel);

        JPanel javaDocPanel = new JPanel(new BorderLayout());
        JBLabel javaDocInfo = new JBLabel("<html><body style='width: 100%;'>" +
            "This will exclude Javadoc comments from the generated context." +
            "This can be useful if you want to focus on the code itself and not the comments." +
            "It will also use less tokens and cheaper to prompt." +
            "</body></html>");
        javaDocInfo.setForeground(UIUtil.getContextHelpForeground());
        javaDocInfo.setBorder(JBUI.Borders.empty(10));

        javaDocPanel.add(javaDocInfo);

        contentPanel.add(javaDocPanel);
        contentPanel.add(createExcludeJavadocPanel());

        panel.add(contentPanel, BorderLayout.CENTER);

        panel.setPreferredSize(new Dimension(400, 500));

        return panel;
    }

    public List<String> getExcludedDirectories() {
        return excludedDirectoriesPanel.getData();
    }

    public List<String> getIncludedFileExtensions() {
        return includedFileExtensionsPanel.getData();
    }

    private @NotNull JPanel createUseGitIgnorePanel() {
        JPanel gitIgnorePanel = new JPanel(new BorderLayout());
        JBLabel gitIgnoreLabel = new JBLabel("<html><body style='width: 100%;'>" +
            "This will exclude all the files and directories specified in the .gitignore file" +
            "</body></html>");
        gitIgnoreLabel.setForeground(UIUtil.getContextHelpForeground());
        gitIgnoreLabel.setBorder(JBUI.Borders.empty(10));
        gitIgnorePanel.add(gitIgnoreLabel);
        gitIgnorePanel.add(useGitIgnoreCheckBox, BorderLayout.SOUTH);

        JPanel useGitIgnorePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        useGitIgnorePanel.add(gitIgnorePanel);
        return useGitIgnorePanel;
    }

    private @NotNull JPanel createExcludeJavadocPanel() {
        JPanel excludeJavadocPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        excludeJavadocPanel.add(excludeJavadocCheckBox);
        return excludeJavadocPanel;
    }

    private static class ExcludedDirectoriesPanel extends AddEditRemovePanel<String> {
        public ExcludedDirectoriesPanel(List<String> initialData) {
            super(new ExcludedDirectoriesModel(), initialData, "Excluded directories");
            setPreferredSize(new Dimension(400, 200));
        }

        @Override
        protected String addItem() {
            return showEditDialog("");
        }

        @Override
        protected boolean removeItem(String item) {
            return true;
        }

        @Override
        protected String editItem(String item) {
            return showEditDialog(item);
        }

        private @Nullable String showEditDialog(String initialValue) {
            JBTextField field = new JBTextField(initialValue);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("Directory:"), BorderLayout.NORTH);
            panel.add(field, BorderLayout.CENTER);
            panel.setBorder(JBUI.Borders.empty(10));

            int result = JOptionPane.showConfirmDialog(this, panel, "Enter Directory", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                return field.getText().trim();
            }
            return null;
        }
    }

    private static class IncludedFileExtensionsPanel extends AddEditRemovePanel<String> {
        public IncludedFileExtensionsPanel(List<String> initialData) {
            super(new IncludedFileExtensionsModel(), initialData, "Included file extensions");
            setPreferredSize(new Dimension(400, 200));
        }

        @Override
        protected String addItem() {
            return showEditDialog("");
        }

        @Override
        protected boolean removeItem(String item) {
            return true;
        }

        @Override
        protected String editItem(String item) {
            return showEditDialog(item);
        }

        private @Nullable String showEditDialog(String initialValue) {
            JBTextField field = new JBTextField(initialValue);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("File Extension:"), BorderLayout.NORTH);
            panel.add(field, BorderLayout.CENTER);
            panel.setBorder(JBUI.Borders.empty(10));

            int result = JOptionPane.showConfirmDialog(this, panel, "Enter File Extension", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                return field.getText().trim();
            }
            return null;
        }
    }

    public boolean getExcludeJavadoc() {
        return excludeJavadocCheckBox.isSelected();
    }

    public List<String> getExcludedFiles() {
        return excludedFilesPanel.getData();
    }

    public boolean getUseGitIgnore() {
        return useGitIgnoreCheckBox.isSelected();
    }

    private static class ExcludedDirectoriesModel extends AddEditRemovePanel.TableModel<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getColumnName(int columnIndex) {
            return "Directory";
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }
    }

    private static class IncludedFileExtensionsModel extends AddEditRemovePanel.TableModel<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getColumnName(int columnIndex) {
            return "Extension";
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }
    }

    private static class ExcludedFilesPanel extends AddEditRemovePanel<String> {
        public ExcludedFilesPanel(List<String> initialData) {
            super(new ExcludedFilesModel(), initialData, "Excluded files");
            setPreferredSize(new Dimension(400, 200));
        }

        @Override
        protected String addItem() {
            return showEditDialog("");
        }

        @Override
        protected boolean removeItem(String item) {
            return true;
        }

        @Override
        protected String editItem(String item) {
            return showEditDialog(item);
        }

        private @Nullable String showEditDialog(String initialValue) {
            JBTextField field = new JBTextField(initialValue);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel("File name:"), BorderLayout.NORTH);
            panel.add(field, BorderLayout.CENTER);
            panel.setBorder(JBUI.Borders.empty(10));

            int result = JOptionPane.showConfirmDialog(this, panel, "Enter File Name", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                return field.getText().trim();
            }
            return null;
        }
    }

    private static class ExcludedFilesModel extends AddEditRemovePanel.TableModel<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Contract(pure = true)
        @Override
        public @NotNull String getColumnName(int columnIndex) {
            return "File Name";
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }
    }
}
