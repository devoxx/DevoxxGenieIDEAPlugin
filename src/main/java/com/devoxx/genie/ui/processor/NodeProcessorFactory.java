package com.devoxx.genie.ui.processor;

import com.devoxx.genie.model.request.ChatMessageContext;
import org.commonmark.node.Block;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class NodeProcessorFactory {

    /**
     * Create a processor for the given block
     * @param chatMessageContext the chat message context
     * @param theBlock the block
     * @return the processor
     */
    @Contract("_, _ -> new")
    public static @NotNull NodeProcessor createProcessor(ChatMessageContext chatMessageContext, Block theBlock) {
        if (theBlock instanceof FencedCodeBlock fencedCodeBlock) {
            return new FencedCodeBlockProcessor(chatMessageContext, fencedCodeBlock);
        } else if (theBlock instanceof IndentedCodeBlock indentedCodeBlock) {
            return new IndentedCodeBlockProcessor(chatMessageContext, indentedCodeBlock);
        } else {
            return new BlockProcessor(chatMessageContext, theBlock);
        }
    }
}
