package com.devoxx.genie.chatmodel.cloud.cloudflare;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue #1256: Cloudflare's {@code /compat} endpoint hands the request to Workers AI, whose native
 * message schema requires every message to carry a <em>string</em> {@code content}. langchain4j
 * omits {@code content} on an assistant message that only carries {@code tool_calls} — valid
 * OpenAI, rejected by Workers AI with
 * {@code required properties at '/messages/N' are 'role,content'} — and sends an array of content
 * parts for multimodal user messages, rejected with
 * {@code Type mismatch of '/messages/N/content', 'array' not in 'string'}.
 */
class CloudflareCompatRequestNormalizerTest {

    @Test
    void otherGatewayProvidersAreLeftCompletelyAlone() {
        // Only Workers AI has the strict message schema. Rewriting requests bound for openai/,
        // anthropic/ etc. would change round trips that work today — those keep the exact bytes
        // langchain4j produced, omitted assistant content and all.
        String body = """
                {"model":"openai/gpt-4o","messages":[
                  {"role":"assistant","tool_calls":[{"id":"call_1","type":"function",
                    "function":{"name":"search_files","arguments":"{}"}}]},
                  {"role":"user","content":[{"type":"text","text":"and now?"}]}
                ]}
                """;

        assertThat(CloudflareCompatRequestNormalizer.normalize(body)).isEqualTo(body);
    }

    @Test
    void assistantToolCallMessageWithoutContentGetsAnEmptyStringContent() {
        String body = """
                {"model":"workers-ai/@cf/openai/gpt-oss-20b","messages":[
                  {"role":"assistant","tool_calls":[{"id":"call_1","type":"function",
                    "function":{"name":"search_files","arguments":"{}"}}]}
                ]}
                """;

        JsonObject normalized = parse(CloudflareCompatRequestNormalizer.normalize(body));

        JsonObject assistant = messages(normalized).get(0).getAsJsonObject();
        assertThat(assistant.get("content").getAsString()).isEmpty();
        assertThat(assistant.getAsJsonArray("tool_calls")).hasSize(1);
    }

    @Test
    void nullContentBecomesAnEmptyString() {
        String body = """
                {"model":"workers-ai/@cf/openai/gpt-oss-20b",
                 "messages":[{"role":"assistant","content":null}]}
                """;

        JsonObject normalized = parse(CloudflareCompatRequestNormalizer.normalize(body));

        assertThat(messages(normalized).get(0).getAsJsonObject().get("content").getAsString()).isEmpty();
    }

    @Test
    void textOnlyContentArrayIsFlattenedIntoAString() {
        String body = """
                {"model":"workers-ai/@cf/openai/gpt-oss-20b",
                 "messages":[{"role":"user","content":[
                  {"type":"text","text":"find null warns"},
                  {"type":"text","text":"in src/"}
                ]}]}
                """;

        JsonObject normalized = parse(CloudflareCompatRequestNormalizer.normalize(body));

        assertThat(messages(normalized).get(0).getAsJsonObject().get("content").getAsString())
                .isEqualTo("find null warns\nin src/");
    }

    @Test
    void contentArrayWithAnImageIsLeftUntouched() {
        // Vision models on the gateway need the parts array; flattening it would drop the image.
        String body = """
                {"model":"workers-ai/@cf/meta/llama-3.2-11b-vision-instruct",
                 "messages":[{"role":"user","content":[
                  {"type":"text","text":"what is this?"},
                  {"type":"image_url","image_url":{"url":"data:image/png;base64,AAA"}}
                ]}]}
                """;

        JsonObject normalized = parse(CloudflareCompatRequestNormalizer.normalize(body));

        assertThat(messages(normalized).get(0).getAsJsonObject().get("content").isJsonArray()).isTrue();
        assertThat(messages(normalized).get(0).getAsJsonObject().getAsJsonArray("content")).hasSize(2);
    }

    @Test
    void stringContentAndEverythingElseIsPreserved() {
        String body = """
                {"model":"workers-ai/@cf/openai/gpt-oss-20b","temperature":0.7,"messages":[
                  {"role":"system","content":"you are helpful"},
                  {"role":"user","content":"find null warns"},
                  {"role":"tool","tool_call_id":"call_1","content":"src/Foo.java:12"}
                ],"tools":[{"type":"function","function":{"name":"search_files"}}]}
                """;

        JsonObject normalized = parse(CloudflareCompatRequestNormalizer.normalize(body));

        assertThat(normalized.get("model").getAsString()).isEqualTo("workers-ai/@cf/openai/gpt-oss-20b");
        assertThat(normalized.get("temperature").getAsDouble()).isEqualTo(0.7);
        assertThat(normalized.getAsJsonArray("tools")).hasSize(1);
        assertThat(messages(normalized).get(0).getAsJsonObject().get("content").getAsString())
                .isEqualTo("you are helpful");
        assertThat(messages(normalized).get(2).getAsJsonObject().get("tool_call_id").getAsString())
                .isEqualTo("call_1");
    }

    @Test
    void nonJsonOrUnrelatedBodiesAreReturnedUnchanged() {
        assertThat(CloudflareCompatRequestNormalizer.normalize(null)).isNull();
        assertThat(CloudflareCompatRequestNormalizer.normalize("not json")).isEqualTo("not json");
        assertThat(CloudflareCompatRequestNormalizer.normalize("{\"input\":\"hi\"}"))
                .isEqualTo("{\"input\":\"hi\"}");
        // No model to identify: leave it be rather than guess.
        assertThat(CloudflareCompatRequestNormalizer.normalize("{\"messages\":[{\"role\":\"assistant\"}]}"))
                .isEqualTo("{\"messages\":[{\"role\":\"assistant\"}]}");
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonArray messages(JsonObject body) {
        return body.getAsJsonArray("messages");
    }
}
