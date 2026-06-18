package com.devoxx.genie.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for the streaming failure:
 *
 * <pre>
 * java.util.ServiceConfigurationError: com.fasterxml.jackson.databind.Module:
 *     com.fasterxml.jackson.module.kotlin.KotlinModule not a subtype
 * </pre>
 *
 * <p>The crash comes from langchain4j's codec calling
 * {@code ObjectMapper.findAndRegisterModules()}, which uses {@code ServiceLoader} against the
 * thread-context classloader. Inside the IntelliJ platform that classloader can see the
 * platform's {@code jackson-module-kotlin} service file while the plugin uses its own
 * {@code jackson-databind}, so the discovered {@code KotlinModule} is "not a subtype" of the
 * {@code Module} type the plugin's Jackson checks against.</p>
 *
 * <p>The dual-classloader clash needs two real Jackson copies and can only be reproduced in a
 * running IDE, so these tests pin the fix's contract instead: the initializer is a no-op-safe
 * idempotent call that leaves the codec usable, and the strategy it relies on — scanning for
 * modules under the JDK platform classloader — discovers nothing, so the offending module is
 * never loaded.</p>
 */
class LangChain4JJsonCodecInitializerTest {

    @Test
    void platformClassLoader_exposesNoJacksonModules_soDiscoveryCannotClash() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            // The JDK platform classloader has no Jackson on it. Running module discovery under
            // it returns an empty list, which is exactly why initializing langchain4j's codec
            // with this context classloader cannot pull in the platform's KotlinModule.
            Thread.currentThread().setContextClassLoader(ClassLoader.getPlatformClassLoader());
            assertThat(ObjectMapper.findModules()).isEmpty();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    void ensureInitialized_isIdempotentAndPreservesContextClassLoader() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        LangChain4JJsonCodecInitializer.ensureInitialized();
        LangChain4JJsonCodecInitializer.ensureInitialized();

        // The initializer must always restore the caller's context classloader.
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    @Test
    void langchain4jJsonCodecIsUsableAfterInitialization() {
        LangChain4JJsonCodecInitializer.ensureInitialized();

        String json = dev.langchain4j.internal.Json.toJson(Map.of("greeting", "hello"));

        assertThat(json).contains("\"greeting\"").contains("\"hello\"");
    }
}
