package com.devoxx.genie.chatmodel;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractLightPlatformTestCase extends LightPlatformTestCase {

    private String originalTmpDir;

    @Override
    protected void setUp() throws Exception {
        originalTmpDir = System.getProperty("java.io.tmpdir");
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
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
