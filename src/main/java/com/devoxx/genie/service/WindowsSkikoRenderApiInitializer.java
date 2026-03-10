package com.devoxx.genie.service;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.util.SystemInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

@Slf4j
public final class WindowsSkikoRenderApiInitializer implements AppLifecycleListener {

    static final String SKIKO_RENDER_API_PROPERTY = "skiko.renderApi";
    static final String SOFTWARE_RENDER_API = "SOFTWARE";

    private final BooleanSupplier isWindows;
    private final BiConsumer<String, String> propertySetter;

    public WindowsSkikoRenderApiInitializer() {
        this(() -> SystemInfo.isWindows, System::setProperty);
    }

    WindowsSkikoRenderApiInitializer(BooleanSupplier isWindows, BiConsumer<String, String> propertySetter) {
        this.isWindows = Objects.requireNonNull(isWindows, "isWindows");
        this.propertySetter = Objects.requireNonNull(propertySetter, "propertySetter");
    }

    @Override
    public void appFrameCreated(List<String> commandLineArgs) {
        initialize();
    }

    void initialize() {
        if (!isWindows.getAsBoolean()) {
            return;
        }

        propertySetter.accept(SKIKO_RENDER_API_PROPERTY, SOFTWARE_RENDER_API);
        log.info("Forced Skiko software rendering on Windows during application startup");
    }
}
