package com.devoxx.genie.service.jan;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseIntellijTest extends BasePlatformTestCase {

    @BeforeEach
    public void setUpTest() throws Exception {
        setUp();
    }

    @AfterEach
    public void tearDownTest() throws Exception {
        tearDown();
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }
}
