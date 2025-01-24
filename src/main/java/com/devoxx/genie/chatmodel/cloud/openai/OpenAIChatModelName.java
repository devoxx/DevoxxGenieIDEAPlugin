package com.devoxx.genie.chatmodel.cloud.openai;

public enum OpenAIChatModelName {
    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_4("gpt-4"),
    GPT_4_32K("gpt-4-32k"),
    GPT_4_O("gpt-4o"),
    GPT_4_O_MINI("gpt-4o-mini"),
    O1_PREVIEW("o1-preview"),
    O1_MINI("o1-mini"),
    O1("o1"),
    O3_MINI("o3-mini"), // Not yet available via API
    O3("o3"); // Not yet available via API

    private final String stringValue;

    OpenAIChatModelName(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
