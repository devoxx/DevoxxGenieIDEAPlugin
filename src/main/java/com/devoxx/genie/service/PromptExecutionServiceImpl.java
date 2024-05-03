package com.devoxx.genie.service;

import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.ui.util.CircularQueue;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PromptExecutionServiceImpl implements PromptExecutionService {

    public static final String YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN =
        "You are a software developer with expert knowledge in ";
    public static final String PROGRAMMING_LANGUAGE = " programming language.";
    public static final String ALWAYS_RETURN_THE_RESPONSE_IN_MARKDOWN = "Always return the response in Markdown.";
    public static final String COMMANDS_INFO = "The Devoxx Genie plugin supports the following commands: /test: write unit tests on selected code\n/explain: explain the selected code\n/review: review selected code\n/custom: set custom prompt in settings";
    public static final String MORE_INFO = "The Devoxx Genie is open source and available at https://github.com/devoxx/DevoxxGenieIDEAPlugin.";
    public static final String NO_HALLUCINATIONS = "Do not include any more info which might be incorrect, like discord, twitter, documentation or website info. Only provide info that is correct and relevant to the code or plugin.";

    public static final String QUESTION = "User question: ";
    public static final String CONTEXT_PROMPT = "Context: \n";

    private final CircularQueue<ChatMessage> chatMessages = new CircularQueue<>(10);

    /**
     * Execute the query with the given language text pair and chat language model.
     * @param promptContext the language text pair
     * @return the response
     */
    @Override
    public @NotNull String executeQuery(@NotNull PromptContext promptContext) throws IllegalAccessException {

        initChatMessage(promptContext);

        try {
            Response<AiMessage> generate = promptContext.getChatLanguageModel().generate(chatMessages.asList());
            String response = generate.content().text();
            chatMessages.add(new AiMessage(response));
            return response;
        } catch (Exception e) {
            throw new IllegalAccessException("Failed to execute prompt!\n" + e.getMessage());
        }
    }

    /**
     * Clear the chat messages.
     */
    @Override
    public void clearChatMessages() {
        chatMessages.removeAll();
    }

    /**
     * Setup the chat message.
     * @param promptContext the language text pair
     */
    private void initChatMessage(PromptContext promptContext) {
        if (chatMessages.isEmpty()) {
            chatMessages.add(createSystemMessage(promptContext));
        }

        String userPrompt = promptContext.getUserPrompt();
        EditorInfo editorInfo = promptContext.getEditorInfo();

        String prompt = QUESTION + userPrompt + "\n\n";
        chatMessages.add(createUserMessageWithContext(prompt, editorInfo, promptContext));
    }

    /**
     * Create a system message.
     * @param promptContext the language text pair
     * @return the system message
     */
    private SystemMessage createSystemMessage(PromptContext promptContext) {
        String language = Optional.ofNullable(promptContext.getEditorInfo())
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
     * @param prompt the prompt
     * @param editorInfo the editor info
     * @param promptContext the language text pair
     * @return the user message
     */
    private UserMessage createUserMessageWithContext(String prompt,
                                                     EditorInfo editorInfo,
                                                     PromptContext promptContext) {
        String context = promptContext.getContext();
        String selectedText = editorInfo != null ? editorInfo.getSelectedText() : "";

        if ((selectedText != null && !selectedText.isEmpty()) ||
            (context != null && !context.isEmpty())) {

            StringBuilder sb = new StringBuilder(prompt);
            sb.append(CONTEXT_PROMPT);

            if (selectedText != null && !selectedText.isEmpty()) {
                sb.append(selectedText).append("\n");
            }

            if (context != null && !context.isEmpty()) {
                sb.append(context);
            }

            prompt = sb.toString();
        }

        return new UserMessage(prompt);
    }
}
