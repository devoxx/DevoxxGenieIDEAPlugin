package com.devoxx.genie.ui.settings.copyproject;

import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.ui.settings.SettingsComponent;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CopyProjectSettingsComponent implements SettingsComponent {

    private final ExcludedDirectoriesPanel excludedDirectoriesPanel;
    private final IncludedFileExtensionsPanel includedFileExtensionsPanel;
    private final JCheckBox excludeJavadocCheckBox;

    public CopyProjectSettingsComponent() {
        DevoxxGenieStateService settings = DevoxxGenieStateService.getInstance();
        excludedDirectoriesPanel = new ExcludedDirectoriesPanel(settings.getExcludedDirectories());
        includedFileExtensionsPanel = new IncludedFileExtensionsPanel(settings.getIncludedFileExtensions());
        excludeJavadocCheckBox = new JCheckBox("Exclude Javadoc", settings.getExcludeJavaDoc());
    }

    @Override
    public JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Add description
        JBLabel descriptionLabel = new JBLabel("<html><body style='width: 100%;'>" +
            "These settings control which files and directories are included when creating a Full Project Context." +
            "Excluded directories will be skipped, and only files with included extensions " +
            "will be processed. This helps to focus the LLM on relevant code and reduce noise from build artifacts " +
            "or other non-essential files.</body></html>");
        descriptionLabel.setForeground(UIUtil.getContextHelpForeground());
        descriptionLabel.setBorder(JBUI.Borders.emptyBottom(10));
        panel.add(descriptionLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(excludedDirectoriesPanel);
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

    @Override
    public void addListeners() {
        // Not needed as listeners are handled in the panel classes
    }

    public List<String> getExcludedDirectories() {
        return excludedDirectoriesPanel.getData();
    }

    public List<String> getIncludedFileExtensions() {
        return includedFileExtensionsPanel.getData();
    }

    private JPanel createExcludeJavadocPanel() {
        JPanel excludeJavadocPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        excludeJavadocPanel.add(excludeJavadocCheckBox);
        return excludeJavadocPanel;
    }

    private static class ExcludedDirectoriesPanel extends AddEditRemovePanel<String> {
        public ExcludedDirectoriesPanel(List<String> initialData) {
            super(new ExcludedDirectoriesModel(), initialData, "Excluded Directories");
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

        private String showEditDialog(String initialValue) {
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
            super(new IncludedFileExtensionsModel(), initialData, "Included File Extensions");
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

    private static class ExcludedDirectoriesModel extends AddEditRemovePanel.TableModel<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
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

        @Override
        public String getColumnName(int columnIndex) {
            return "Extension";
        }

        @Override
        public Object getField(String o, int columnIndex) {
            return o;
        }
    }
}
