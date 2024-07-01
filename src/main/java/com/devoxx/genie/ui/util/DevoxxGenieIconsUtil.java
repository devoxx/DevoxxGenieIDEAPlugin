package com.devoxx.genie.ui.util;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class DevoxxGenieIconsUtil {

    public static final Icon CogIcon = load("/icons/cog.svg");
    public static final Icon PlusIcon = load("/icons/plus.svg");
    public static final Icon AddFileIcon = load("/icons/addNewFile.svg");
    public static final Icon CalculateIcon = load("/icons/calculator.svg");
    public static final Icon DeleteIcon = load("/icons/delete.svg");
    public static final Icon SubmitIcon = load("/icons/paperPlane.svg");
    public static final Icon WebSearchIcon = load("/icons/web.svg");
    public static final Icon GoogleIcon = load("/icons/google-small.svg");
    public static final Icon ArrowExpand = load("/icons/arrowExpand.svg");
    public static final Icon ArrowExpanded = load("/icons/arrowExpanded.svg");
    public static final Icon CloseSmalllIcon = load("/icons/closeSmall_dark.svg");
    public static final Icon StopIcon = load("/icons/stop.svg");
    public static final Icon InterfaceIcon = load("/icons/interface.svg");
    public static final Icon EnumIcon = load("/icons/enum.svg");
    public static final Icon ClassIcon = load("/icons/class.svg");
    public static final Icon TrashIcon = load("/icons/trash.svg");
    public static final Icon DevoxxIcon = load("/icons/pluginIcon.svg");
    public static final Icon CodeSnippetIcon = load("/icons/codeSnippet.svg");
    public static final Icon CopyIcon = load("/icons/copy.svg");
    public static final Icon InsertCodeIcon = load("/icons/insertCode.svg");

    private DevoxxGenieIconsUtil() {
    }

    private static Icon load(String path) {
        return IconLoader.getIcon(path, DevoxxGenieIconsUtil.class);
    }

}
