package com.devoxx.genie.controller.listener;

public interface ProjectContextListener {

    void onProjectContextAdded(int tokenCount, int tokenLimit);

    void onProjectContextRemoved();
}