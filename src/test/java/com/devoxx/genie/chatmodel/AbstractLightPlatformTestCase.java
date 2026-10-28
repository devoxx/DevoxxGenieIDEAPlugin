package com.devoxx.genie.chatmodel;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;

public abstract class AbstractLightPlatformTestCase extends LightPlatformTestCase {

    private String originalTmpDir;
    private boolean platformSetUp;

    @Override
    protected void setUp() throws Exception {
        originalTmpDir = System.getProperty("java.io.tmpdir");
        super.setUp();
        platformSetUp = true;
    }

    /**
     * Annotated so JUnit 5 subclasses dispose the platform fixture: Jupiter never calls the
     * JUnit 3 tearDown() on its own, and each undisposed fixture nests the next test's temp
     * directory one level deeper until setUp() fails with "File name too long".
     * Runs on the EDT (as runBare() would) and is skipped for subclasses that never call setUp().
     */
    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        if (!platformSetUp) {
            return;
        }
        platformSetUp = false;
        try {
            EdtTestUtil.runInEdtAndWait(() -> super.tearDown());
        } finally {
            if (originalTmpDir != null) {
                System.setProperty("java.io.tmpdir", originalTmpDir);
            }
        }
    }

    @Override
    protected @NotNull String getTestName(boolean lowercaseFirstLetter) {
        @NotNull String name = getName() != null ? getName() : "defaultTestName";
        name = StringUtil.trimStart(name, "test");
        if (!name.isEmpty() && lowercaseFirstLetter && !PlatformTestUtil.isAllUppercaseName(name)) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
