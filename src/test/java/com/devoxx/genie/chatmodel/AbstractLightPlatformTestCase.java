package com.devoxx.genie.chatmodel;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.LightPlatformTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractLightPlatformTestCase extends LightPlatformTestCase {

    @Override
    protected @NotNull String getTestName(boolean lowercaseFirstLetter) {
        String name = getName();
        if (name == null) {
            name = "defaultTestName";
        }
        name = StringUtil.trimStart(name, "test");
        if (!name.isEmpty() && lowercaseFirstLetter && !PlatformTestUtil.isAllUppercaseName(name)) {
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
