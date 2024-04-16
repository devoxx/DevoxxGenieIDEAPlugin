package com.devoxx.genie.model;

public class Constant {
    private Constant() {
    }
    public static String TEST_PROMPT = "Write a unit test for this code using JUnit.";
    public static String REVIEW_PROMPT = "Review the selected code, can it be improved or are there bugs?";
    public static String EXLAIN_PROMPT = "Explain the code so a junior developer can understand it.";

    public static String OLLAMA_MODEL_URL = "http://localhost:11434/";
    public static String LMSTUDIO_MODEL_URL = "http://localhost:1234/v1/";
    public static String GPT4ALL_MODEL_URL = "http://localhost:4891/v1/";

    public static Double TEMPERATURE = 0.7d;
    public static Double TOP_P = 0.9d;
    public static Integer MAX_RETRIES = 3;
    public static Integer TIMEOUT = 60;
}
