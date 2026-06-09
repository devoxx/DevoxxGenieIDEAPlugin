package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceCallChainsToolExecutorTest extends BasePlatformTestCase {

    private TraceCallChainsToolExecutor executor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        executor = new TraceCallChainsToolExecutor(getProject());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static ToolExecutionRequest request(String args) {
        return ToolExecutionRequest.builder().name("trace_call_chains").arguments(args).build();
    }

    private void addChainFixture() {
        // 1: public class Chain {
        // 2:     void a() { b(); }
        // 3:     void b() { c(); }
        // 4:     void c() {}
        // 5: }
        myFixture.addFileToProject("Chain.java",
                "public class Chain {\n" +
                "    void a() { b(); }\n" +
                "    void b() { c(); }\n" +
                "    void c() {}\n" +
                "}\n");
    }

    // --- Input validation ---

    @Test
    public void missingFile_returnsError() {
        assertThat(executor.execute(request("{\"line\": 2}"), null)).contains("Error").contains("file");
    }

    @Test
    public void missingLine_returnsError() {
        assertThat(executor.execute(request("{\"file\": \"Foo.java\"}"), null)).contains("Error").contains("line");
    }

    // --- AC #8: a known caller chain is traced within the depth bound ---

    @Test
    public void tracesCallerChain() {
        addChainFixture();
        // start at c() (line 4), direction defaults to callers: c <- b <- a
        String result = executor.execute(request("{\"file\": \"Chain.java\", \"line\": 4}"), null);

        assertThat(result).contains("a()");
        assertThat(result).contains("b()");
        assertThat(result).contains("c()");
        assertThat(result).contains("caller chain");
    }

    @Test
    public void tracesCalleeChainToTarget() {
        addChainFixture();
        // start at a() (line 2), direction callees, target c: a -> b -> c
        String result = executor.execute(
                request("{\"file\": \"Chain.java\", \"line\": 2, \"direction\": \"callees\", \"target\": \"c\"}"), null);

        assertThat(result).contains("reaching 'c'");
        assertThat(result).contains("a()");
        assertThat(result).contains("b()");
        assertThat(result).contains("c()");
    }

    @Test
    public void depthIsBounded() {
        addChainFixture();
        // depth 1 from c(): cannot reach a() two hops away
        String result = executor.execute(request("{\"file\": \"Chain.java\", \"line\": 4, \"depth\": 1}"), null);
        assertThat(result).doesNotContain("a()");
    }
}
