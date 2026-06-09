package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FindDeadCodeToolExecutorTest extends BasePlatformTestCase {

    private FindDeadCodeToolExecutor executor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        executor = new FindDeadCodeToolExecutor(getProject());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static ToolExecutionRequest request(String args) {
        return ToolExecutionRequest.builder().name("find_dead_code").arguments(args).build();
    }

    // --- Input validation ---

    @Test
    public void missingFile_returnsError() {
        assertThat(executor.execute(request("{}"), null)).contains("Error").contains("file");
    }

    // --- AC #8: unreferenced private method is a candidate; @Override / entry-point is not ---

    @Test
    public void reportsUnreferencedPrivateButNotOverrideOrPublic() {
        myFixture.addFileToProject("D.java",
                "public class D {\n" +
                "    private void unusedPrivate() {}\n" +
                "    private void usedPrivate() {}\n" +
                "    public void entry() { usedPrivate(); }\n" +
                "    @Override public String toString() { return \"x\"; }\n" +
                "}\n");

        String result = executor.execute(request("{\"file\": \"D.java\"}"), null);

        // The unreferenced private method is a candidate.
        assertThat(result).contains("[method] unusedPrivate");
        // Heuristic labelling is present.
        assertThat(result).contains("CANDIDATES").contains("confirm before deleting");
        // Referenced private method is NOT a candidate.
        assertThat(result).doesNotContain("[method] usedPrivate");
        // Public entry point and @Override toString are excluded.
        assertThat(result).doesNotContain("[method] entry");
        assertThat(result).doesNotContain("[method] toString");
    }

    @Test
    public void allReferencedOrExcluded_reportsNone() {
        myFixture.addFileToProject("Clean.java",
                "public class Clean {\n" +
                "    private int used() { return 1; }\n" +
                "    public int api() { return used(); }\n" +
                "}\n");

        String result = executor.execute(request("{\"file\": \"Clean.java\"}"), null);
        assertThat(result).contains("No dead-code candidates");
    }
}
