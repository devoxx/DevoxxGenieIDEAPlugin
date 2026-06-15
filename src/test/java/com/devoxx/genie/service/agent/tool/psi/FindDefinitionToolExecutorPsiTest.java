package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-PSI regression tests for {@link FindDefinitionToolExecutor}, guarding the robustness fixes
 * that let the tool cope with the imperfect (file, line, symbol) arguments LLMs typically supply:
 * <ul>
 *     <li>a slightly wrong package directory in the path (resolved by filename fallback), and</li>
 *     <li>a symbol name with an imprecise / wrong line number (resolved by file-wide symbol search).</li>
 * </ul>
 */
class FindDefinitionToolExecutorPsiTest extends BasePlatformTestCase {

    private FindDefinitionToolExecutor executor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        executor = new FindDefinitionToolExecutor(getProject());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static ToolExecutionRequest request(String args) {
        return ToolExecutionRequest.builder().name("find_definition").arguments(args).build();
    }

    // --- Resolving a usage to its definition (precise position) ---

    @Test
    public void resolvesUsageToDefinition() {
        // 1: public class Sample {
        // 2:     void caller() {
        // 3:         target();
        // 4:     }
        // 5:     void target() {}
        // 6: }
        myFixture.addFileToProject("Sample.java",
                "public class Sample {\n" +
                "    void caller() {\n" +
                "        target();\n" +
                "    }\n" +
                "    void target() {}\n" +
                "}\n");

        String result = executor.execute(
                request("{\"file\": \"Sample.java\", \"line\": 3, \"symbol\": \"target\"}"), null);

        assertThat(result).contains("Definition of 'target'");
        assertThat(result).contains("Sample.java:5");
    }

    // --- FIX 1: symbol with a wrong/imprecise line still resolves (file-wide fallback) ---

    @Test
    public void resolvesSymbolWhenLineIsWrong() {
        // The reported failure: symbol points at the class, but the line points elsewhere
        // (a body line that does not mention the symbol at all).
        myFixture.addFileToProject("ProjectContextController.java",
                "public class ProjectContextController {\n" +     // line 1 - the declaration
                "    private final Object project;\n" +
                "    public ProjectContextController(Object project) {\n" +
                "        this.project = project;\n" +              // line 4 - LLM points here
                "    }\n" +
                "}\n");

        String result = executor.execute(
                request("{\"file\": \"ProjectContextController.java\", \"line\": 4, " +
                        "\"symbol\": \"ProjectContextController\"}"), null);

        assertThat(result).contains("Definition of 'ProjectContextController'");
        assertThat(result).contains("ProjectContextController.java:1");
    }

    @Test
    public void resolvesMethodSymbolFromWrongLine() {
        myFixture.addFileToProject("Calc.java",
                "public class Calc {\n" +
                "    int compute() { return 1; }\n" +   // line 2 - definition
                "    int unrelated() { return 2; }\n" +  // line 3 - LLM points here
                "}\n");

        String result = executor.execute(
                request("{\"file\": \"Calc.java\", \"line\": 3, \"symbol\": \"compute\"}"), null);

        assertThat(result).contains("Definition of 'compute'");
        assertThat(result).contains("Calc.java:2");
    }

    // --- FIX 2: wrong package directory in the path still resolves (filename fallback) ---

    @Test
    public void resolvesFileWithWrongPackageDirectory() {
        myFixture.addFileToProject("src/main/java/com/example/controller/Widget.java",
                "package com.example.controller;\n" +
                "public class Widget {\n" +
                "    void use() { build(); }\n" +
                "    void build() {}\n" +
                "}\n");

        // The path says 'ui' but the file actually lives under 'controller'.
        String result = executor.execute(
                request("{\"file\": \"src/main/java/com/example/ui/Widget.java\", " +
                        "\"line\": 3, \"symbol\": \"build\"}"), null);

        assertThat(result).contains("Definition of 'build'");
        assertThat(result).contains("Widget.java:4");
    }

    // --- A genuinely unknown symbol still reports a clear error ---

    @Test
    public void unknownSymbolReturnsError() {
        myFixture.addFileToProject("Empty.java",
                "public class Empty {\n" +
                "    void m() {}\n" +
                "}\n");

        String result = executor.execute(
                request("{\"file\": \"Empty.java\", \"line\": 2, \"symbol\": \"doesNotExist\"}"), null);

        assertThat(result).contains("Error").contains("Could not resolve symbol");
    }
}
