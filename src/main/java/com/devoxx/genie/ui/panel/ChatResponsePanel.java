package com.devoxx.genie.ui.panel;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.FileListManager;
import com.devoxx.genie.ui.component.ExpandablePanel;
import com.devoxx.genie.ui.processor.NodeProcessorFactory;
import com.devoxx.genie.ui.settings.DevoxxGenieStateService;
import com.devoxx.genie.util.DefaultLLMSettingsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.commonmark.node.Block;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

import static com.devoxx.genie.ui.util.DevoxxGenieFontsUtil.SourceCodeProFontPlan14;

public class ChatResponsePanel extends BackgroundPanel {

    private final ChatMessageContext chatMessageContext;

    /**
     * Create a new chat response panel.
     * @param chatMessageContext the chat message context
     */
    public ChatResponsePanel(@NotNull ChatMessageContext chatMessageContext) {
        super(chatMessageContext.getName());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.chatMessageContext = chatMessageContext;

        add(new ResponseHeaderPanel(chatMessageContext));
        addResponsePane(chatMessageContext);
    }

    /**
     * Get the response pane with rendered HTML.
     *
     * @param chatMessageContext the chat message context
     */
    private void addResponsePane(@NotNull ChatMessageContext chatMessageContext) {
        String markDownResponse = chatMessageContext.getAiMessage().text();
        Node document = Parser.builder().build().parse(markDownResponse);
        addDocumentNodesToPanel(document);

        if (chatMessageContext.hasFiles()) {
            java.util.List<VirtualFile> files = FileListManager.getInstance().getFiles();
            ExpandablePanel fileListPanel = new ExpandablePanel(chatMessageContext, files);
            add(fileListPanel);
        }

        if (DevoxxGenieStateService.getInstance().getShowExecutionTime()) {
            // Add execution time, token usage and cost information
            addTokenUsageAndCostInfo(chatMessageContext);
        }
    }

    private void addTokenUsageAndCostInfo(@NotNull ChatMessageContext chatMessageContext) {
        dev.langchain4j.model.output.TokenUsage tokenUsage = chatMessageContext.getTokenUsage();
        if (tokenUsage != null) {
            JPanel tokenInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            tokenInfoPanel.setOpaque(false);

            String cost = "";
            if (DefaultLLMSettingsUtil.isApiKeyBasedProvider(chatMessageContext.getLanguageModel().getProvider())) {
                cost = String.format("- %.5f $", chatMessageContext.getCost());
            }

            String extraInfoString = String.format("ϟ %.2fs - Tokens ↑ %d ↓️ %d %s",
                                            chatMessageContext.getExecutionTimeMs() / 1000.0,
                                            tokenUsage.inputTokenCount(),
                                            tokenUsage.outputTokenCount(),
                                            cost);

            JLabel tokenLabel = new JLabel(extraInfoString);

            tokenLabel.setForeground(JBColor.GRAY);
            tokenLabel.setFont(tokenLabel.getFont().deriveFont(12f));

            tokenInfoPanel.add(tokenLabel);
            add(tokenInfoPanel);
        }
    }

    /**
     * Add document nodes to the panel.
     *
     * @param document the document
     */
    private void addDocumentNodesToPanel(@NotNull Node document) {
        JPanel jPanel = createPanel();

        Node node = document.getFirstChild();

        while (node != null) {
            JPanel panel;
            if (node instanceof FencedCodeBlock fencedCodeBlock) {
                panel = processBlock(fencedCodeBlock);
            } else if (node instanceof IndentedCodeBlock indentedCodeBlock) {
                panel = processBlock(indentedCodeBlock);
            } else {
                panel = processBlock((Block) node);
            }

            setFullWidth(panel);
            jPanel.add(panel);
            node = node.getNext();
        }

        add(jPanel);
    }

    private void setFullWidth(@NotNull JPanel panel) {
        Dimension maximumSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        panel.setMaximumSize(maximumSize);
        panel.setMinimumSize(new Dimension(panel.getPreferredSize().width, panel.getPreferredSize().height));
    }

    /**
     * Create a panel.
     *
     * @return the panel
     */
    private @NotNull JPanel createPanel() {
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jPanel.setOpaque(false);
        jPanel.setFont(SourceCodeProFontPlan14);
        return jPanel;
    }

    /**
     * Process a block and return a panel.
     *
     * @param theBlock the block
     * @return the panel
     */
    private JPanel processBlock(Block theBlock) {
        return NodeProcessorFactory.createProcessor(chatMessageContext, theBlock).processNode();
    }
}
