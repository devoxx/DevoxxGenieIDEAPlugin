package com.devoxx.genie.service.prompt.threading;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThreadPoolShutdownManagerTest {

    private static final String CLASS_NAME =
            "com.devoxx.genie.service.prompt.threading.ThreadPoolShutdownManager";

    /**
     * Forces the class's static initializer to run while a mocked Application is available,
     * so referencing it later (to invoke the private shutdown method) cannot NPE inside the
     * static block itself. The initializer subscribes to the message bus and registers a JVM
     * shutdown hook.
     */
    private void forceClassInitialized(@org.jetbrains.annotations.NotNull Application mockApp,
                                       @org.jetbrains.annotations.NotNull MockedStatic<ApplicationManager> appMgr)
            throws Exception {
        MessageBus mockBus = mock(MessageBus.class);
        MessageBusConnection mockConn = mock(MessageBusConnection.class);
        when(mockApp.getMessageBus()).thenReturn(mockBus);
        when(mockBus.connect()).thenReturn(mockConn);
        appMgr.when(ApplicationManager::getApplication).thenReturn(mockApp);
        Class.forName(CLASS_NAME, true, getClass().getClassLoader());
    }

    private Method shutdownMethod() throws Exception {
        Method m = Class.forName(CLASS_NAME).getDeclaredMethod("shutdownThreadPools");
        m.setAccessible(true);
        return m;
    }

    @Test
    void shutdownThreadPools_whenApplicationIsNull_returnsWithoutTouchingThreadPoolManager() throws Exception {
        try (MockedStatic<ApplicationManager> appMgr = mockStatic(ApplicationManager.class);
             MockedStatic<ThreadPoolManager> tpm = mockStatic(ThreadPoolManager.class)) {

            Application mockApp = mock(Application.class);
            forceClassInitialized(mockApp, appMgr);

            // Simulate the JVM shutdown hook firing after the Application has been torn down.
            appMgr.when(ApplicationManager::getApplication).thenReturn(null);

            Method shutdown = shutdownMethod();

            // The guard must swallow the missing Application cleanly — no NPE from getInstance().
            assertThatCode(() -> shutdown.invoke(null)).doesNotThrowAnyException();
            tpm.verify(ThreadPoolManager::getInstance, never());
        }
    }

    @Test
    void shutdownThreadPools_whenApplicationPresent_shutsDownThreadPools() throws Exception {
        try (MockedStatic<ApplicationManager> appMgr = mockStatic(ApplicationManager.class);
             MockedStatic<ThreadPoolManager> tpm = mockStatic(ThreadPoolManager.class)) {

            Application mockApp = mock(Application.class);
            forceClassInitialized(mockApp, appMgr);

            ThreadPoolManager mockManager = mock(ThreadPoolManager.class);
            tpm.when(ThreadPoolManager::getInstance).thenReturn(mockManager);

            shutdownMethod().invoke(null);

            verify(mockManager).shutdown();
        }
    }
}
