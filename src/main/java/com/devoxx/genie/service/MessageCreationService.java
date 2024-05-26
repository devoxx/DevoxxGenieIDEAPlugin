package com.devoxx.genie.service;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.model.request.EditorInfo;
import com.intellij.openapi.application.ApplicationManager;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MessageCreationService {

    public static final String YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN =
        "You are a software developer with expert knowledge in ";
    public static final String PROGRAMMING_LANGUAGE = " programming language.";
    public static final String ALWAYS_RETURN_THE_RESPONSE_IN_MARKDOWN =
        "Always return the response in Markdown.";
    public static final String COMMANDS_INFO =
        "The Devoxx Genie plugin supports the following commands: /test: write unit tests on selected code\n/explain: explain the selected code\n/review: review selected code\n/custom: set custom prompt in settings";
    public static final String MORE_INFO =
        "The Devoxx Genie is open source and available at https://github.com/devoxx/DevoxxGenieIDEAPlugin.";
    public static final String NO_HALLUCINATIONS =
        "Do not include any more info which might be incorrect, like discord, twitter, documentation or website info. Only provide info that is correct and relevant to the code or plugin.";

    public static final String QUESTION = "Answer the user question: ";
    public static final String CONTEXT_PROMPT = "Question context: \n";

    @NotNull
    public static MessageCreationService getInstance() {
        return ApplicationManager.getApplication().getService(MessageCreationService.class);
    }

    /**
     * Create a system message.
     *
     * @param chatMessageContext the language text pair
     * @return the system message
     */
    public @NotNull SystemMessage createSystemMessage(@NotNull ChatMessageContext chatMessageContext) {
        String language = Optional.ofNullable(chatMessageContext.getEditorInfo())
            .map(EditorInfo::getLanguage)
            .orElse("programming");

        return new SystemMessage(
            YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN +
                language + PROGRAMMING_LANGUAGE +
                ALWAYS_RETURN_THE_RESPONSE_IN_MARKDOWN + "\n" +
                COMMANDS_INFO + "\n" +
                MORE_INFO + "\n" +
                NO_HALLUCINATIONS);
    }

    /**
     * Create a user message with context.
     *
     * @param chatMessageContext the chat message context
     */
    public @NotNull UserMessage createUserMessage(@NotNull ChatMessageContext chatMessageContext) {
        UserMessage userMessage;
        String context = chatMessageContext.getContext();
        String selectedText = chatMessageContext.getEditorInfo() != null ? chatMessageContext.getEditorInfo().getSelectedText() : "";
        if ((selectedText != null && !selectedText.isEmpty()) ||
            (context != null && !context.isEmpty())) {
            StringBuilder sb = new StringBuilder(QUESTION);
            addContext(chatMessageContext, sb, selectedText, context);
            userMessage = new UserMessage(sb.toString());
            chatMessageContext.setUserMessage(userMessage);
        } else {
            userMessage = new UserMessage(QUESTION + " " + chatMessageContext.getUserPrompt());
        }
        return userMessage;
    }

    /**
     * Add prompt context (selected code snippet) to the chat message.
     *
     * @param chatMessageContext the chat message context
     * @param sb                 the string builder
     * @param selectedText       the selected text
     * @param context            the context
     */
    private static void addContext(@NotNull ChatMessageContext chatMessageContext,
                                   @NotNull StringBuilder sb,
                                   String selectedText,
                                   String context) {
        appendIfNotEmpty(sb, chatMessageContext.getUserPrompt());
        appendIfNotEmpty(sb, CONTEXT_PROMPT);
        appendIfNotEmpty(sb, selectedText);
        appendIfNotEmpty(sb, context);
    }

    /**
     * Append the text to the string builder if it is not empty.
     *
     * @param sb   the string builder
     * @param text the text
     */
    private static void appendIfNotEmpty(StringBuilder sb, String text) {
        if (text != null && !text.isEmpty()) {
            sb.append(text).append("\n");
        }
    }

}
