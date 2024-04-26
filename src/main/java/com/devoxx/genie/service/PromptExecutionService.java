package com.devoxx.genie.service;

import com.devoxx.genie.model.PromptContext;
import com.devoxx.genie.ui.util.CircularQueue;
import com.intellij.openapi.vfs.VirtualFile;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.stream.Collectors;

public class PromptExecutionService {

    public static final String YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN =
        "You are a software developer with expert knowledge in ";

    public static final String PROGRAMMING_LANGUAGE = " programming language.";

    private final CircularQueue<ChatMessage> chatMessages = new CircularQueue<>(10);

    /**
     * Execute the query with the given language text pair and chat language model.
     * @param promptContext the language text pair
     * @param chatLanguageModel the chat language model
     * @return the response
     */
    public String executeQuery(PromptContext promptContext, ChatLanguageModel chatLanguageModel)
        throws IllegalAccessException {

        setupChatMessage(promptContext);

        try {
            Response<AiMessage> generate = chatLanguageModel.generate(chatMessages.asList());
            String response = generate.content().text();
            chatMessages.add(new AiMessage(response));

            return convertHTMLResponseToMarkdown(response, promptContext.getFiles());
        } catch (Exception e) {
            throw new IllegalAccessException("Failed to execute prompt!\n" + e.getMessage());
        }
    }

    /**
     * Update the UI with the response.  Convert the response to markdown to improve readability.
     * @param response the response
     * @return the markdown text
     */
    private String convertHTMLResponseToMarkdown(String response, List<VirtualFile> files) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        response = addFilesContextInfo(response, files);

        Node document = parser.parse(response);
        return renderer.render(document);
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
            chatMessages.add(new SystemMessage(
                YOU_ARE_A_SOFTWARE_DEVELOPER_WITH_EXPERT_KNOWLEDGE_IN + promptContext.getLanguage() + PROGRAMMING_LANGUAGE +
                    "Always return the response in Markdown."));
        }

        String userPrompt = promptContext.getPrompt() + "\n\nSelected code: " + promptContext.getText();
        chatMessages.add(new UserMessage(userPrompt));
    }
}
