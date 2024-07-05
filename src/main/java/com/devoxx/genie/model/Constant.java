package com.devoxx.genie.model;

public class Constant {

    private Constant() {
    }

    // The fixed command prompts
    public static final String SYSTEM_PROMPT = """
        You are a software developer with expert knowledge in any programming language.

        The Devoxx Genie plugin supports the following commands:
        /test: write unit tests on selected code
        /explain: explain the selected code
        /review: review selected code

        The Devoxx Genie is open source and available at https://github.com/devoxx/DevoxxGenieIDEAPlugin.
        You can follow us on Twitter @DevoxxGenie.
        Do not include any more info which might be incorrect, like discord, documentation or other websites.
        Only provide info that is correct and relevant to the code or plugin.
        """;

    public static final String MARKDOWN = "\nAlways use markdown to format your prompt. For example, use **bold** or *italic* text and ``` code blocks ```.";

    public static final String TEST_PROMPT = "Write a unit test for this code using JUnit.";
    public static final String REVIEW_PROMPT = "Review the selected code, can it be improved or are there any bugs?";
    public static final String EXPLAIN_PROMPT = "Break down the code in simple terms to help a junior developer grasp its functionality.";

    // The Local LLM Model URLs, these can be overridden in the settings page
    public static final String OLLAMA_MODEL_URL = "http://localhost:11434/";
    public static final String LMSTUDIO_MODEL_URL = "http://localhost:1234/v1/";
    public static final String GPT4ALL_MODEL_URL = "http://localhost:4891/v1/";
    public static final String JAN_MODEL_URL = "http://localhost:1337/v1/";

    // ActionCommands
    public static final String SUBMIT_ACTION = "submit";
    public static final String TAVILY_SEARCH_ACTION = "tavilySearch";
    public static final String GOOGLE_SEARCH_ACTION = "googleSearch";
    public static final String COMBO_BOX_CHANGED = "comboBoxChanged";

    // I18N file name
    public static final String MESSAGES = "messages";

    // The LLM Settings
    public static final Double TEMPERATURE = 0.7d;
    public static final Double TOP_P = 0.9d;
    public static final Integer MAX_OUTPUT_TOKENS = 2500;
    public static final Integer MAX_RETRIES = 3;
    public static final Integer TIMEOUT = 60;
    public static final Integer MAX_MEMORY = 10;

    // Hide Search Button
    public static final Boolean HIDE_SEARCH_BUTTONS = false;
    public static final Integer MAX_SEARCH_RESULTS = 3;

    // Stream mode settings
    public static final Boolean STREAM_MODE = false;

    // AST settings
    public static final Boolean AST_MODE = false;
    public static final Boolean AST_PARENT_CLASS = true;
    public static final Boolean AST_CLASS_REFERENCE = true;
    public static final Boolean AST_FIELD_REFERENCE = true;

    // Button tooltip texts
    public static final String ADD_FILE_S_TO_PROMPT_CONTEXT = "Add file(s) to prompt context";
    public static final String SUBMIT_THE_PROMPT = "Submit the prompt";
    public static final String SEARCH_THE_WEB_WITH_TAVILY_FOR_AN_ANSWER = "Search the web with Tavily for an answer";
    public static final String SEARCH_GOOGLE_FOR_AN_ANSWER = "Search Google for an answer";
    public static final String PROMPT_IS_RUNNING_PLEASE_BE_PATIENT = "Prompt is running, please be patient...";
    public static final String STOP_STREAMING = "Stop streaming response";

}
