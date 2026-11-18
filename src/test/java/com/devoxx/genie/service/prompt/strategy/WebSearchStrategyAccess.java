package com.devoxx.genie.service.prompt.strategy;

import com.devoxx.genie.model.request.ChatMessageContext;
import com.devoxx.genie.service.prompt.result.PromptResult;
import com.devoxx.genie.service.prompt.threading.PromptTask;
import com.devoxx.genie.ui.panel.PromptOutputPanel;

/** Test-only bridge: runs the /search strategy's work from end-to-end tests in other packages. */
public final class WebSearchStrategyAccess {

    private WebSearchStrategyAccess() {
    }

    public static void run(ChatMessageContext context, PromptOutputPanel panel, PromptTask<PromptResult> resultTask) {
        new WebSearchPromptStrategy(context.getProject()).executeStrategySpecific(context, panel, resultTask);
    }
}
