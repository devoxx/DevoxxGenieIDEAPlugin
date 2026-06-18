package com.devoxx.genie.util;

import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * Forces langchain4j's static JSON codec to initialize once, early, under a controlled
 * thread-context classloader.
 *
 * <p><strong>Why this exists.</strong> langchain4j-core builds its singleton Jackson
 * {@code ObjectMapper} ({@code dev.langchain4j.internal.JacksonJsonCodec}) by calling
 * {@code ObjectMapper.findAndRegisterModules()}. That method uses
 * {@code ServiceLoader} with the current thread-context classloader to discover every
 * {@code com.fasterxml.jackson.databind.Module} on the classpath.</p>
 *
 * <p>Inside the IntelliJ Platform there are two Jackson copies: the platform bundles
 * jackson (and {@code jackson-module-kotlin}) while this plugin bundles its own
 * {@code jackson-databind} pulled in transitively by langchain4j. When the codec
 * initializes on a thread whose context classloader can see the platform's
 * {@code jackson-module-kotlin} service file, {@code ServiceLoader} loads the platform's
 * {@code KotlinModule} and checks it against the plugin's {@code Module} class. Because the
 * two come from different classloaders the check fails with:</p>
 *
 * <pre>
 * java.util.ServiceConfigurationError: com.fasterxml.jackson.databind.Module:
 *     com.fasterxml.jackson.module.kotlin.KotlinModule not a subtype
 * </pre>
 *
 * <p>which surfaces to the user as a streaming failure. langchain4j registers the modules it
 * actually needs explicitly, so the auto-discovered Kotlin module is irrelevant to it.</p>
 *
 * <p><strong>The fix.</strong> Trigger the codec's class initialization while the
 * thread-context classloader is the JDK platform classloader, which has no Jackson on it.
 * {@code findAndRegisterModules()} then discovers zero modules, never touches the platform's
 * {@code KotlinModule}, and the resulting codec is cached for the rest of the JVM session.
 * The work is idempotent and must run before any langchain4j JSON (de)serialization, so it is
 * invoked both at plugin startup and defensively before every chat-model creation.</p>
 */
@Slf4j
public final class LangChain4JJsonCodecInitializer {

    /** Fully-qualified name of the langchain4j class whose static initializer builds the codec. */
    private static final String JSON_CLASS = "dev.langchain4j.internal.Json";

    private static boolean initialized = false;

    private LangChain4JJsonCodecInitializer() {
    }

    /**
     * Initializes the langchain4j JSON codec exactly once. Safe to call from any thread and as
     * often as needed; only the first invocation does any work.
     */
    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        // Mark done up front: the codec is a one-shot static singleton, so even if the trigger
        // below throws there is no value in retrying (and retries would spam the log).
        initialized = true;

        Thread current = Thread.currentThread();
        ClassLoader previous = current.getContextClassLoader();
        try {
            current.setContextClassLoader(ClassLoader.getPlatformClassLoader());
            // Loading with initialize=true runs Json's static initializer, which builds the
            // Jackson codec under our safe context classloader.
            Class.forName(JSON_CLASS, true, ChatModel.class.getClassLoader());
            log.debug("langchain4j JSON codec pre-initialized");
        } catch (Throwable t) {
            // Never let codec warm-up break model creation; worst case the original behaviour
            // (and its potential ServiceConfigurationError) is unchanged.
            log.warn("Failed to pre-initialize langchain4j JSON codec", t);
        } finally {
            current.setContextClassLoader(previous);
        }
    }
}
