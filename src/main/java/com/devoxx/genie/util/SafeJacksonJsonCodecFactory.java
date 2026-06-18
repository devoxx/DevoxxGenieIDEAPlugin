package com.devoxx.genie.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import dev.langchain4j.internal.Json;
import dev.langchain4j.spi.json.JsonCodecFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

/**
 * Custom langchain4j {@link JsonCodecFactory} that prevents the
 * {@code ServiceConfigurationError: com.fasterxml.jackson.databind.Module:
 * com.fasterxml.jackson.module.kotlin.KotlinModule not a subtype} crash that
 * occurs during streaming inside the IntelliJ Platform.
 *
 * <h3>Root cause</h3>
 * langchain4j's default {@code dev.langchain4j.internal.JacksonJsonCodec} builds
 * its {@code ObjectMapper} via {@code ObjectMapper.findAndRegisterModules()},
 * which runs a {@link java.util.ServiceLoader} scan for every Jackson
 * {@code Module} provider visible to the thread-context classloader. Inside
 * IntelliJ that scan discovers the IDE platform's {@code jackson-module-kotlin}
 * provider. Because this plugin bundles and loads its <b>own</b> copy of
 * {@code jackson-databind}, the {@code KotlinModule} (loaded by the platform
 * classloader, linked against the platform's {@code jackson-databind}) is not an
 * {@code instanceof} the {@code Module} type from the plugin's
 * {@code jackson-databind}. {@code ServiceLoader} then throws the
 * "not a subtype" error.
 *
 * <h3>Fix</h3>
 * This factory is registered via SPI in
 * {@code META-INF/services/dev.langchain4j.spi.json.JsonCodecFactory}. When a
 * factory is present, langchain4j's {@code Json#loadCodec()} uses it instead of
 * constructing the default {@code new JacksonJsonCodec()}, so
 * {@code findAndRegisterModules()} is never invoked. We build an equivalent
 * {@code ObjectMapper} ourselves, registering only the modules we control.
 *
 * <p>{@code dev.langchain4j.internal.JacksonJsonCodec} is package-private, so we
 * cannot reuse it directly. Instead we provide an equivalent {@link Json.JsonCodec}
 * implementation ({@link SafeJacksonJsonCodec}) whose {@code ObjectMapper}
 * configuration (visibility, (de)serialization features and the ISO-8601
 * {@code java.time} (de)serializers) mirrors langchain4j's own
 * {@code JacksonJsonCodec#createObjectMapper()}, so the behaviour of
 * {@code dev.langchain4j.internal.Json} stays identical &mdash; only the
 * cross-classloader {@code ServiceLoader} scan is removed.</p>
 */
public class SafeJacksonJsonCodecFactory implements JsonCodecFactory {

    @Override
    public Json.JsonCodec create() {
        return new SafeJacksonJsonCodec(createObjectMapper());
    }

    private static ObjectMapper createObjectMapper() {
        SimpleModule javaTimeModule = new SimpleModule("devoxxgenie-langchain4j-time");

        javaTimeModule.addSerializer(LocalDate.class, isoSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        javaTimeModule.addDeserializer(LocalDate.class, deserializer(LocalDate::parse));

        javaTimeModule.addSerializer(LocalTime.class, isoSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
        javaTimeModule.addDeserializer(LocalTime.class, deserializer(LocalTime::parse));

        javaTimeModule.addSerializer(LocalDateTime.class, isoSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDateTime.class, deserializer(LocalDateTime::parse));

        return JsonMapper.builder()
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .disable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(javaTimeModule)
                .build();
    }

    private static <T extends TemporalAccessor> JsonSerializer<T> isoSerializer(DateTimeFormatter formatter) {
        return new JsonSerializer<>() {
            @Override
            public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(formatter.format(value));
            }
        };
    }

    private static <T> JsonDeserializer<T> deserializer(Function<String, T> parser) {
        return new JsonDeserializer<>() {
            @Override
            public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return parser.apply(p.getValueAsString());
            }
        };
    }

    /**
     * Equivalent of langchain4j's package-private {@code JacksonJsonCodec}: a thin
     * wrapper that delegates to a pre-built {@link ObjectMapper}, wrapping checked
     * Jackson exceptions in {@link RuntimeException} exactly as langchain4j does.
     */
    private record SafeJacksonJsonCodec(ObjectMapper objectMapper) implements Json.JsonCodec {

        @Override
        public String toJson(Object o) {
            try {
                return objectMapper.writeValueAsString(o);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T fromJson(String json, Class<T> type) {
            try {
                return objectMapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T fromJson(String json, Type type) {
            try {
                return objectMapper.readValue(json, objectMapper.constructType(type));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
