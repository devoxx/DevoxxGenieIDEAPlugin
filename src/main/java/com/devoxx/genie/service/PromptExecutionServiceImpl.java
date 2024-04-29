package com.devoxx.genie.service;

import com.devoxx.genie.model.request.CompletionResult;
import com.devoxx.genie.model.request.EditorInfo;
import com.devoxx.genie.model.request.PromptContext;
import com.devoxx.genie.ui.util.CircularQueue;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class PromptExecutionServiceImpl implements PromptExecutionService {

    public static final String YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN =
        "You are a software developer with expert knowledge in ";

    public static final String PROGRAMMING_LANGUAGE = " programming language.";
    public static final String ALWAYS_RETURN_THE_RESPONSE_IN_MARKDOWN = "Always return the response in Markdown.";

    private final CircularQueue<ChatMessage> chatMessages = new CircularQueue<>(10);

    /**
     * Execute the query with the given language text pair and chat language model.
     * @param promptContext the language text pair
     * @return the response
     */
    @Override
    public @NotNull CompletionResult executeQuery(@NotNull PromptContext promptContext) throws IllegalAccessException {

        setupChatMessage(promptContext);

        try {
            Response<AiMessage> generate = promptContext.getChatLanguageModel().generate(chatMessages.asList());
            String response = generate.content().text();
            chatMessages.add(new AiMessage(response));

            return convertHTMLResponseToMarkdown(response, promptContext.getEditorInfo());
        } catch (Exception e) {
            throw new IllegalAccessException("Failed to execute prompt!\n" + e.getMessage());
        }
    }

    /**
     * Update the UI with the response.  Convert the response to markdown to improve readability.
     * @param response the response
     * @return the markdown text
     */
    private CompletionResult convertHTMLResponseToMarkdown(String response, EditorInfo editorInfo) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        if (editorInfo!= null && editorInfo.getSelectedFiles() != null) {
            response = addFilesContextInfo(response, editorInfo.getSelectedFiles());
        }

        Node document = parser.parse(response);

        return new CompletionResult(renderer.render(document));
    }

    /**
     * Add the files used for the prompt context to the response.
     * @param response the response
     * @param files the files
     * @return the response with the files context info
     */
    private String addFilesContextInfo(String response, List<VirtualFile> files) {
        if (files != null && !files.isEmpty()) {
            StringBuilder responseBuilder = new StringBuilder(response);
            responseBuilder.append("\n\n**Files used for prompt context:**\n");

            String fileNames = files.stream()
                .map(VirtualFile::getName)
                .collect(Collectors.joining("\n- ", "- ", ""));

            responseBuilder.append(fileNames);
            return responseBuilder.toString();
        }
        return response;
    }

    /**
     * Setup the chat message.
     * @param promptContext the language text pair
     */
    private void setupChatMessage(PromptContext promptContext) {
        if (chatMessages.isEmpty()) {
            if (promptContext.getEditorInfo() != null) {
                chatMessages.add(new SystemMessage(
                    YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN +
                        promptContext.getEditorInfo().getLanguage() + PROGRAMMING_LANGUAGE +
                        ALWAYS_RETURN_THE_RESPONSE_IN_MARKDOWN));
            } else {
                chatMessages.add(new SystemMessage(
                    YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN +
                        " programmming " +
                        ALWAYS_RETURN_THE_RESPONSE_IN_MARKDOWN));
            }
        }

        String userPrompt = promptContext.getUserPrompt();

        EditorInfo editorInfo = promptContext.getEditorInfo();

        if (editorInfo == null) {
            chatMessages.add(new UserMessage(userPrompt));
        } else {
            if (editorInfo.getSelectedText() != null) {
                String userPromptWithCode = userPrompt + "\n\nSelected code: " + editorInfo.getSelectedText();
                chatMessages.add(new UserMessage(userPromptWithCode));
            } else {
                chatMessages.add(new UserMessage(userPrompt));
            }
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
