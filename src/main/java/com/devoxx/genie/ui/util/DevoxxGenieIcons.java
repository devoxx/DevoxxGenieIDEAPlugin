package com.devoxx.genie.ui.util;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class DevoxxGenieIcons {

    public static final Icon ClockIcon = load("/icons/clock.svg");
    public static final Icon SubmitIcon = load("/icons/paper-plane.svg");
    public static final Icon PlusIcon = load("/icons/plus.svg");

    private DevoxxGenieIcons() {
    }

    private static Icon load(String path) {
        return IconLoader.getIcon(path, DevoxxGenieIcons.class);
    }

}
