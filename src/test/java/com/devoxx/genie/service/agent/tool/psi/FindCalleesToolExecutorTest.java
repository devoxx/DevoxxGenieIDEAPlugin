package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindCalleesToolExecutorTest extends BasePlatformTestCase {

    private FindCalleesToolExecutor executor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        executor = new FindCalleesToolExecutor(getProject());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static ToolExecutionRequest request(String args) {
        return ToolExecutionRequest.builder().name("find_callees").arguments(args).build();
    }

    // --- Input validation (no fixture needed) ---

    @Test
    public void missingFile_returnsError() {
        assertThat(executor.execute(request("{\"line\": 2}"), null)).contains("Error").contains("file");
    }

    @Test
    public void missingLine_returnsError() {
        assertThat(executor.execute(request("{\"file\": \"Foo.java\"}"), null)).contains("Error").contains("line");
    }

    @Test
    public void invalidJson_returnsError() {
        assertThat(executor.execute(request("not json"), null)).contains("Error");
    }

    @Test
    public void fileNotFound_returnsError() {
        assertThat(executor.execute(request("{\"file\": \"Nope.java\", \"line\": 1}"), null))
                .contains("Error").contains("File not found");
    }

    // --- Real PSI resolution (AC #8: callees of a method are resolved) ---

    @Test
    public void resolvesOutgoingCalls() {
        // 1: public class Caller {
        // 2:     void source() {
        // 3:         target();
        // 4:         helper();
        // 5:     }
        // 6:     void target() {}
        // 7:     void helper() {}
        // 8: }
        myFixture.addFileToProject("Caller.java",
                "public class Caller {\n" +
                "    void source() {\n" +
                "        target();\n" +
                "        helper();\n" +
                "    }\n" +
                "    void target() {}\n" +
                "    void helper() {}\n" +
                "}\n");

        String result = executor.execute(request("{\"file\": \"Caller.java\", \"line\": 2}"), null);

        assertThat(result).contains("2 distinct method(s)");
        assertThat(result).contains("target()");
        assertThat(result).contains("helper()");
    }

    @Test
    public void methodWithNoCalls_reportsNone() {
        myFixture.addFileToProject("Leaf.java",
                "public class Leaf {\n" +
                "    int value() { return 42; }\n" +
                "}\n");

        String result = executor.execute(request("{\"file\": \"Leaf.java\", \"line\": 2}"), null);
        assertThat(result).contains("No resolved method calls");
    }

    @Test
    public void notAMethodLine_returnsError() {
        myFixture.addFileToProject("Plain.java", "public class Plain {\n    int field = 1;\n}\n");
        String result = executor.execute(request("{\"file\": \"Plain.java\", \"line\": 1}"), null);
        assertThat(result).contains("Error").contains("method");
    }
}
