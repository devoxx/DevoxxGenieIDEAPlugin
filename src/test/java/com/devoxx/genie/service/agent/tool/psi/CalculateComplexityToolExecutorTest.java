package com.devoxx.genie.service.agent.tool.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculateComplexityToolExecutorTest extends BasePlatformTestCase {

    private CalculateComplexityToolExecutor executor;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        executor = new CalculateComplexityToolExecutor(getProject());
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private static ToolExecutionRequest request(String args) {
        return ToolExecutionRequest.builder().name("calculate_complexity").arguments(args).build();
    }

    // --- Input validation ---

    @Test
    public void missingFile_returnsError() {
        assertThat(executor.execute(request("{}"), null)).contains("Error").contains("file");
    }

    @Test
    public void fileNotFound_returnsError() {
        assertThat(executor.execute(request("{\"file\": \"Nope.java\"}"), null))
                .contains("Error").contains("File not found");
    }

    // --- AC #8: complexity of a method with known decision points matches the expected count ---

    @Test
    public void complexityOfMatchesKnownDecisionPoints() {
        // base 1 + if(1) + && (1) + for(1) + else-if(1) = 5
        PsiFile file = myFixture.addFileToProject("Cx.java",
                "public class Cx {\n" +
                "    int m(int x) {\n" +
                "        if (x > 0 && x < 10) {\n" +
                "            for (int i = 0; i < x; i++) { }\n" +
                "        } else if (x < 0) { }\n" +
                "        return x;\n" +
                "    }\n" +
                "}\n");

        int complexity = ReadAction.compute(() -> {
            PsiMethod m = PsiTreeUtil.collectElementsOfType(file, PsiMethod.class).stream()
                    .filter(x -> "m".equals(x.getName())).findFirst().orElseThrow();
            return CalculateComplexityToolExecutor.complexityOf(m);
        });

        assertThat(complexity).isEqualTo(5);
    }

    @Test
    public void trivialMethodHasComplexityOne() {
        PsiFile file = myFixture.addFileToProject("Trivial.java",
                "public class Trivial {\n" +
                "    int id(int x) { return x; }\n" +
                "}\n");

        int complexity = ReadAction.compute(() -> {
            PsiMethod m = PsiTreeUtil.collectElementsOfType(file, PsiMethod.class).stream()
                    .filter(x -> "id".equals(x.getName())).findFirst().orElseThrow();
            return CalculateComplexityToolExecutor.complexityOf(m);
        });

        assertThat(complexity).isEqualTo(1);
    }

    @Test
    public void executeFlagsMethodsOverThreshold() {
        myFixture.addFileToProject("Cx2.java",
                "public class Cx2 {\n" +
                "    int m(int x) {\n" +
                "        if (x > 0 && x < 10) {\n" +
                "            for (int i = 0; i < x; i++) { }\n" +
                "        } else if (x < 0) { }\n" +
                "        return x;\n" +
                "    }\n" +
                "}\n");

        // threshold 3 → method (complexity 5) is flagged
        String result = executor.execute(request("{\"file\": \"Cx2.java\", \"threshold\": 3}"), null);
        assertThat(result).contains("Cx2.m");
        assertThat(result).contains("over threshold");
    }
}
