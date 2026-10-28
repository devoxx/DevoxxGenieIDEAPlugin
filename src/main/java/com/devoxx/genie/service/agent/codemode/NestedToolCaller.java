package com.devoxx.genie.service.agent.codemode;

@FunctionalInterface
interface NestedToolCaller {

    String call(String toolName, String argumentsJson);
}
