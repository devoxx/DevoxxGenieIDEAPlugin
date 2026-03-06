package com.devoxx.genie.service.automation.listeners;

import com.devoxx.genie.model.automation.EventContext;
import com.devoxx.genie.model.automation.IdeEventType;
import com.devoxx.genie.service.automation.EventAutomationService;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestStatusListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Listens for test execution results and fires TEST_FAILED or TEST_SUITE_PASSED automations.
 * This is an application-level listener registered via plugin.xml extension point.
 */
@Slf4j
public class TestExecutionListener extends TestStatusListener {

    @Override
    public void testSuiteFinished(@Nullable AbstractTestProxy root) {
        if (root == null) {
            return;
        }

        EventAutomationService automationService = EventAutomationService.getInstance();
        if (automationService == null) {
            return;
        }

        // Collect failed tests
        List<AbstractTestProxy> failedTests = new ArrayList<>();
        collectFailed(root, failedTests);

        // Resolve project — use the first non-disposed open project
        Project project = findActiveProject();
        if (project == null) {
            return;
        }

        if (!failedTests.isEmpty()) {
            handleTestsFailed(project, failedTests, root, automationService);
        } else {
            handleTestsPassed(project, root, automationService);
        }
    }

    private void handleTestsFailed(@Nullable Project project,
                                   @Nullable List<AbstractTestProxy> failedTests,
                                   @Nullable AbstractTestProxy root,
                                   @Nullable EventAutomationService automationService) {
        if (project == null || failedTests == null || automationService == null) {
            return;
        }

        StringBuilder content = new StringBuilder();
        for (AbstractTestProxy test : failedTests) {
            content.append("FAILED: ").append(test.getName()).append('\n');
            if (test.isLeaf()) {
                String errorMessage = test.getName();
                content.append("  ").append(errorMessage).append('\n');
            }
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("failedCount", String.valueOf(failedTests.size()));
        metadata.put("totalCount", String.valueOf(countLeafTests(root)));
        metadata.put("suiteName", root != null ? root.getName() : "unknown");

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.TEST_FAILED)
                .content(content.toString().trim())
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);
    }

    private void handleTestsPassed(@Nullable Project project,
                                   @Nullable AbstractTestProxy root,
                                   @Nullable EventAutomationService automationService) {
        if (project == null || root == null || automationService == null) {
            return;
        }

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("totalCount", String.valueOf(countLeafTests(root)));
        metadata.put("suiteName", root.getName());

        EventContext ctx = EventContext.builder()
                .eventType(IdeEventType.TEST_SUITE_PASSED)
                .metadata(metadata)
                .build();

        automationService.onEvent(project, ctx);
    }

    private void collectFailed(@Nullable AbstractTestProxy node, @Nullable List<AbstractTestProxy> results) {
        if (node == null || results == null) {
            return;
        }
        if (node.isLeaf() && node.getMagnitude() >= 6) { // 6 = FAILED in TestProxy magnitude
            results.add(node);
        }
        for (AbstractTestProxy child : node.getChildren()) {
            collectFailed(child, results);
        }
    }

    private int countLeafTests(@Nullable AbstractTestProxy node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        int count = 0;
        for (AbstractTestProxy child : node.getChildren()) {
            count += countLeafTests(child);
        }
        return count;
    }

    private @Nullable Project findActiveProject() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (!project.isDisposed() && !project.isDefault()) {
                return project;
            }
        }
        return null;
    }
}
