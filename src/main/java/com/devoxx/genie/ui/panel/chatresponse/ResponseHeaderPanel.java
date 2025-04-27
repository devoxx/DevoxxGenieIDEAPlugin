package com.devoxx.genie.ui.panel.chatresponse;

import com.devoxx.genie.model.LanguageModel;
import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.ui.util.FontUtil;
import com.devoxx.genie.ui.util.NotificationUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.time.format.DateTimeFormatter;

import static com.devoxx.genie.chatmodel.ChatModelFactory.TEST_MODEL;
import static com.devoxx.genie.ui.component.button.ButtonFactory.createActionButton;
import static com.devoxx.genie.ui.util.DevoxxGenieIconsUtil.CopyIcon;

public class ResponseHeaderPanel extends JBPanel<ResponseHeaderPanel> {

    /**
     * The response header panel.
     *
     * @param chatMessageContext the chat message context
     */
    public ResponseHeaderPanel(@NotNull ChatMessageContext chatMessageContext) {
        super(new BorderLayout());
        setBackground(JBColor.BLUE);
        andTransparent()
            .withMaximumHeight(30)
            .withPreferredHeight(30);
            
        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createCopyButton(chatMessageContext));

        add(buttonPanel, BorderLayout.EAST);
    }
    /**
     * Get the created on label.
     *
     * @param chatMessageContext the chat message context
     * @return the created on label
     */
    private static @NotNull JBLabel getCreatedOnLabel(@NotNull ChatMessageContext chatMessageContext) {

        LanguageModel languageModel = chatMessageContext.getLanguageModel();

        String modelInfo = languageModel.getProvider().getName();
        String modelName = languageModel.getModelName();
        if (modelName != null && !modelName.isBlank() && !modelName.equalsIgnoreCase(TEST_MODEL)) {
            modelInfo += " (" + languageModel.getModelName() + ")";
        }

        String label = chatMessageContext.getCreatedOn().format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm")) + " : " + modelInfo;

        JBLabel createdOnLabel = new JBLabel(label, SwingConstants.LEFT);
        createdOnLabel.setFont(createdOnLabel.getFont().deriveFont(FontUtil.getFontSize()));

        createdOnLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
        return createdOnLabel;
    }

    /**
     * Create the Copy button to copy prompt response.
     *
     * @param chatMessageContext the chat message context
     * @return the Delete button
     */
    private @NotNull JButton createCopyButton(ChatMessageContext chatMessageContext) {
        return createActionButton(CopyIcon, "Copy to clipboard", e -> copyPrompt(chatMessageContext));
    }

    /**
     * Copy the prompt response to the system clipboard.
     *
     * @param chatMessageContext the chat message context
     */
    private void copyPrompt(@NotNull ChatMessageContext chatMessageContext) {
        String response = chatMessageContext.getAiMessage().text();
        Transferable transferable = new StringSelection(response);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
        NotificationUtil.sendNotification(
                chatMessageContext.getProject(),
                "The prompt response has been copied to the clipboard"
        );
    }
}
